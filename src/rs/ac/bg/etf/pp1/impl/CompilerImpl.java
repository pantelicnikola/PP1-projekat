package rs.ac.bg.etf.pp1.impl;

import java_cup.runtime.*;
import org.apache.log4j.*;

import rs.ac.bg.etf.pp1.MJParserTest;
import rs.etf.pp1.mj.runtime.Code;
import rs.etf.pp1.symboltable.Tab;
import rs.etf.pp1.symboltable.concepts.*;

public class CompilerImpl {

	private static Obj currentProgram;
	private static Obj currentMethod;
	private static String currentMethodName;
	private static boolean returned;
	private static Obj currentClass;
	private static Struct currentType;
	private static Struct currentReturnType;
	
	public static boolean designatorIsArray;
	
	private static Scope universeScope;
	
	private static int globalVars;
	private static int localVars;
	private static int globalArrays;
	private static int localArrays;
	private static int globalConstants;
	
	public static final int _ARRAY = Struct.Array;
	public static final int _STRING = 6;
	public static final int _BOOL = Struct.Bool;
	
	public static final Struct stringType = new Struct(_STRING, Tab.charType);
    public static final Struct boolType = new Struct(_BOOL);
    public static final Struct arrayType = new Struct(_ARRAY, Tab.intType);
    
    
    public static int globalAdr = 0;
    public static int localAdr = 0;
	
	Logger log = Logger.getLogger(CompilerImpl.class);
	
	public void setType(Struct t) {
		currentType = t;
	}
	
	public Struct getType(String typeName, int typeNameleft) {
		Obj typeNode = Tab.find(typeName);
		if (typeNode == Tab.noObj) {
			log.error("Nije pronadjen tip " + typeName + " u tabeli simbola, linija: " + typeNameleft);
			return Tab.noType;
		} else {
			if (Obj.Type == typeNode.getKind()) {
				return typeNode.getType();
			} else {
				log.error("Greska na liniji" + typeNameleft+ ": Ime " + typeName + " ne predstavlja tip ");
				return Tab.noType;
			}
		}
	}

	public void startProgram(String pName) {
		globalVars = 0;
		localVars = 0;
		globalConstants = 0;
		globalArrays = 0;
		
		Tab.insert(Obj.Type, "string", stringType);
		Tab.insert(Obj.Type, "bool", boolType);
		
		currentProgram = Tab.insert(Obj.Prog, pName, Tab.noType);
		
		Tab.openScope();
		
		universeScope = Tab.currentScope();
	}

	public void endProgram() {
		Tab.chainLocalSymbols(currentProgram);
		Tab.closeScope();
		currentProgram = null;
		log.info("Global vars: " + globalVars);
		log.info("Local vars: " + localVars);
		log.info("Global arrays: " + globalArrays);
		log.info("Local arrays: " + localArrays);
		log.info("Global constants: " + globalConstants);
	}

	

	public void insertGlobalVar(String varName, int varNameleft) {
		if(Tab.currentScope().findSymbol(varName) == null) {
			globalVars++;
			Tab.insert(Obj.Var, varName, currentType);
		} else {
			log.error("Promenljiva " + varName + " je vec definisana - linija: " + varNameleft);
		}
		
	}

	public void insertGlobalArray(String varName, int varNameleft) {
		if(Tab.currentScope().findSymbol(varName) == null){
			globalArrays++;
			Tab.insert(Obj.Var, varName, new Struct(Struct.Array, currentType));
			log.info("Deklarisan je globalni niz " + varName + " - linija: " + varNameleft);
		} else {
			log.error("Niz " + varName + " je vec definisan - linija: " + varNameleft);
		}
		
	}
	
	public void insertLocalVar(String varName, int varNameleft) {
		if(Tab.currentScope().findSymbol(varName) == null){
			localVars++;
			Tab.insert(Obj.Var, varName, currentType);
		} else {
			log.error("Promenljiva " + varName + " je vec definisana - linija: " + varNameleft);
		}
		
	}

	public void insertLocalArray(String varName, int varNameleft) {
		if(Tab.currentScope().findSymbol(varName) == null){
			localArrays++;
			Obj var = Tab.insert(Obj.Var, varName, new Struct(Struct.Array, currentType));
			var.setAdr(localAdr);
			localAdr++;
			log.info("Deklarisan je lokalni niz " + varName + " - linija: " + varNameleft);
		} else {
			log.error("Niz " + varName + " je vec definisan - linija: " + varNameleft);
		}
		
	}

	public void insertConstant(String constName, Object constValue, int constNameleft){
		
		if(Tab.currentScope().findSymbol(constName) == null){
			int adr = 0;
			
			if (constValue instanceof Integer && currentType.getKind() == Struct.Int) {
				adr = (Integer) constValue;
			} else if (constValue instanceof Character && currentType.getKind() == Struct.Char) {
				adr = (int) ((Character) (constValue));
			} else if (constValue instanceof Boolean && currentType.getKind() == Struct.Bool) {
				adr =  1;
			} else {
				log.error("Vrednost " + constValue + " nije komaptibilna sa tipom konstante - linija: " + constNameleft);
				return;
			}
			
			globalConstants++;
			Tab.insert(Obj.Var, constName, currentType).setAdr(adr);
			
			log.info("Deklarisana je konstanta " + constName + " - linija: " + constNameleft);
		} else {
			log.error("Konstanta " + constName + " je vec definisana - linija: " + constNameleft);
		}
	}

	
	public void insertMethod(Struct retType, String methName, int retTypeleft){
		returned = false;
		if(Tab.currentScope().findSymbol(methName) != null){
			log.error("Metoda " + methName + " je vec definisana - linija: " + retTypeleft);
		} else  {
			if(retType == null){
				currentReturnType = Tab.noType;
				currentMethod = Tab.insert(Obj.Meth, methName, Tab.noType);
			} else {
				currentMethod = Tab.insert(Obj.Meth, methName, retType);
				currentReturnType = retType;
			}
			
			
			Tab.openScope();
			currentMethodName = methName;
		}	
		
	}
	
	
	
	
	public void checkReturn(Obj expr, int line){
		returned = true;
		
		if(currentReturnType == Tab.noType){
			if (expr != null) {
				log.error("VOID funkcija ne moze da vraca vrednost - linija: " + line);
				return;
			}
		} else {
			if (expr.getType().getKind() != currentReturnType.getKind()) {
				log.error("Povratna vrednost metode ne odgovara vracenoj vrednosti - linija: " + line);
				return;
			}			
		}
	}
	
	
	public void insertMethodArg(Struct type, String name, int line){
		if (Tab.currentScope().findSymbol(name) == null) {
			Tab.insert(Obj.Var, name, type);
		} else {
			log.error("Duplikat imena argumenata - linija: " + line);
		}
				
	}
	
	
	
	public void endMethod(){
		if (currentReturnType != Tab.noType && !returned) {
			log.error("Metoda " + currentMethod.getName() + " nema RETURN iskaz");
		} else {
			Tab.chainLocalSymbols(currentMethod);
			Tab.closeScope();
			currentMethod = null;
			currentMethodName = null;
			currentReturnType = null;
		}
		
	}
	
	
	
	
	
	public void insertClass(String className,int classNameleft){
		if(Tab.currentScope().findSymbol(className) == null){
			currentClass = Tab.insert(Obj.Type, className, new Struct(Struct.Class)); // ovo mozda ne valja
			Tab.openScope();
			
		} else {
			log.error("Klasa " + className + " je vec definisana - linija: " + classNameleft);
		}
		
	}
	
	public void endClass(){
		Tab.chainLocalSymbols(currentClass);
		Tab.closeScope();
		currentClass = null;
	}
	
	
	
	
	
	public Obj findDesignator(String name, int nameleft){
		
		Obj obj = Tab.currentScope().findSymbol(name);
		if(obj == Tab.noObj || obj == null){
			obj = universeScope.findSymbol(name);
			if (obj == Tab.noObj || obj == null){
				log.error("Promenljiva " + name + " nije definisana - linija: " + nameleft);
				return null;
			}
		}
		return obj;
	}
	
	public void read(Object des, int line){
		Obj d = (Obj) des;
		if(d.getType() == Tab.intType || d.getType().getKind() == Struct.Bool ){
			Code.put(Code.read);
			Code.store(d);
		} else if (d.getType() == Tab.charType){
			Code.put(Code.bread);
			Code.store(d);
		} else {
			log.error("Operand instrukcije READ mora bili int, char ili bool - linija: " + line);
		}
	}
	
	public void print(Object e, int line){
		if (e != Tab.intType && e != Tab.charType){
	  		log.error("Operand instruckije PRINT mora biti int ili char - linija: " + line);
	  	} 
	  	if (e == Tab.intType){
	  		Code.loadConst(5);
	  		Code.put(Code.print);
	  	}
	  	
	  	if (e == Tab.charType){
	  		Code.loadConst(1);
	  		Code.put(Code.bprint);
	  	}
	}
	
	public void printN(Object e, int num, int line){
		if (e != Tab.intType && e != Tab.charType){
	  		log.error("Operand instruckije PRINT mora biti int ili char - linija: " + line);
	  	} 
	  	if (e == Tab.intType){
	  		Code.loadConst(num);
	  		Code.put(Code.print);
	  	}
	  	
	  	if (e == Tab.charType){
	  		Code.loadConst(num);
	  		Code.put(Code.bprint);
	  	}
	}
	
	
	
	
	
	
	
	
	public Obj factorInsertFunc(Obj des, int line){
		Obj d = (Obj) des;
		if(Obj.Meth == d.getKind()){
			if(d.getType() != Tab.noType){
				int adr = d.getAdr() - Code.pc;
				Code.put(Code.call);
				Code.put2(adr);
			}
			return d;
		} else {
			log.error("Funkcija " + d.getName() + " nije definisana - linija: " + line);
			return Tab.noObj;
		}
	}
	
	
	
	public Obj factorInsertNum(Integer num){
		Obj o = new Obj(Obj.Con,"",new Struct(Struct.Int));
		o.setAdr(num.intValue());
		return o;		
	}
	
	public Obj factorInsertChar(Character chr){
		Obj o = new Obj(Obj.Con,"",new Struct(Struct.Char));
		o.setAdr(chr.charValue());
		return o;
	}

	public Obj factorInsertBool(Boolean bool){
		Obj o = new Obj(Obj.Con,"",new Struct(Struct.Bool));
		o.setAdr(bool.booleanValue() ? 1:0);
		return o;
	}
	
	public void execAssign(Obj des, Obj expr, Integer op, int line) {
		checkComaptibility(des, expr, op, line);
	}

	public Obj execAddopLeft(Obj terms, Obj term, Integer op, int line) {
		return checkComaptibility(terms, term, op, line);
	}

	public Obj execMulopRight(Obj terms, Obj termsWrapper, Integer op, int line) {
		return checkComaptibility(terms, termsWrapper, op, line);
	}

	public Obj execAddopRight(Obj terms, Obj termsWrapper, Integer op, int line) {
		return checkComaptibility(terms, termsWrapper, op, line);
	}

	public Obj execMulopLeft(Obj term, Obj factor, Integer op, int line) {
		return checkComaptibility(term, factor, op, line);
	}
	
	public Obj checkComaptibility(Obj first, Obj second, Integer op, int line) {
		
		if (first.getType().getKind() != Struct.Int && op != 0) {
			log.error("Ilegalna operacija - linija: " + line);
			return Tab.noObj;
		}
		
		if (first.getType().getKind() == second.getType().getKind()) {
			return first;
		} else {
			log.error("Tipovi nisu kompatibilni - linija: " + line);
			return Tab.noObj;
		}
	}

	

}
