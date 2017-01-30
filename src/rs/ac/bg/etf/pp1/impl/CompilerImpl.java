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
	private static Obj currentClass;
	private static Struct currentType;
	private static Struct currentReturnType;
	
	private static int globalVars;
	private static int localVars;
	private static int globalArrays;
	private static int localArrays;
	private static int globalConstants;
	
	public static final int _STRING = 6;
	public static final int _BOOL = 5;
	
	public static final Struct stringType = new Struct(_STRING, Tab.charType);
    public static final Struct boolType = new Struct(_BOOL);
    
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
		if(Tab.currentScope().findSymbol(varName) == null){
			globalVars++;
			Obj var = Tab.insert(Obj.Var, varName, currentType);
			var.setAdr(globalAdr);
			globalAdr++;
			log.info("Deklarisana je globalna promenljiva " + varName + " - linija: " + varNameleft);
		} else {
			log.error("Promenljiva " + varName + " je vec definisana - linija: " + varNameleft);
		}
		
	}

	public void insertGlobalArray(String varName, int varNameleft) {
		if(Tab.currentScope().findSymbol(varName) == null){
			globalArrays++;
			Obj var = Tab.insert(Obj.Var, varName, currentType);
			var.setAdr(globalAdr);
			globalAdr++;
			log.info("Deklarisan je globalni niz " + varName + " - linija: " + varNameleft);
		} else {
			log.error("Niz " + varName + " je vec definisan - linija: " + varNameleft);
		}
		
	}
	
	public void insertLocalVar(String varName, int varNameleft) {
		if(Tab.currentScope().findSymbol(varName) == null){
			localVars++;
			Obj var = Tab.insert(Obj.Var, varName, currentType);
			var.setAdr(localAdr);
			localAdr++;
			log.info("Deklarisana je lokalna promenljiva " + varName + " - linija: " + varNameleft);
		} else {
			log.error("Promenljiva " + varName + " je vec definisana - linija: " + varNameleft);
		}
		
	}

	public void insertLocalArray(String varName, int varNameleft) {
		if(Tab.currentScope().findSymbol(varName) == null){
			localArrays++;
			Obj var = Tab.insert(Obj.Var, varName, currentType);
			var.setAdr(localAdr);
			localAdr++;
			log.info("Deklarisan je lokalni niz " + varName + " - linija: " + varNameleft);
		} else {
			log.error("Niz " + varName + " je vec definisan - linija: " + varNameleft);
		}
		
	}

	
	
	public void insertMethod(Object retType, String methName, int retTypeleft){
		
		if(Tab.currentScope().findSymbol(methName) != null){
			log.error("Metoda " + methName + " je vec definisana - linija: " + retTypeleft);
		} else  {
			if(retType == null){
				currentReturnType = Tab.noType;
				currentMethod = Tab.insert(Obj.Meth, methName, Tab.noType);
			} else {
				currentMethod = Tab.insert(Obj.Meth, methName, (Struct)retType);
				currentReturnType = (Struct) retType;
			}
			
			
			Tab.openScope();
			log.info("Obradjuje se funkcija " + methName + " - linija: " + retTypeleft);
			currentMethodName = methName;
		}	
		
	}
	
	public void startMethod(){
		currentMethod.setAdr(Code.pc);
		if(currentMethodName.equals("main")){
			Code.mainPc = currentMethod.getAdr();
		}
		Code.put(Code.enter);
		Code.put(currentMethod.getLevel()); // broj argumenata
		Code.put(Tab.currentScope().getnVars()); // broj lok. promenljivih
	}
	
	
	public void checkReturn(Object expr, int line){
		Struct type = (Struct)expr;
		if(expr == null){
			type = Tab.noType;
		} else {
			type = (Struct) expr;
		}

		
		if(type == Tab.noType){
			log.info("U pitanju je void funkcija");
		} else {
			
			if(!type.assignableTo(currentMethod.getType())){
				log.error("Pogresna povratna vrednost u metodi " + currentMethod.getName() + " linija - " + line);
			}
		}
	}
	
	
	public void insertMethodArg(Struct type, String name, int line){
		Tab.insert(Obj.Var, name, type);
	}
	
	
	
	public void endMethod(){
		Code.put(Code.exit);
		Code.put(Code.return_);
		Tab.chainLocalSymbols(currentMethod);
		Tab.closeScope();
		currentMethod = null;
		currentMethodName = null;
	}
	
	
	
	
	
	public void insertClass(String className,int classNameleft){
		if(Tab.currentScope().findSymbol(className) == null){
			currentClass = Tab.insert(Obj.Type, className, Tab.noType); // ovo mozda ne valja
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
	
	public void insertConstant(String constName, Object constValue, int constNameleft){
		
		if(Tab.currentScope().findSymbol(constName) == null){
			
			
			
			int adr = 0;
			
			if (constValue instanceof Integer && currentType.getKind() == Struct.Int) {
				adr = (Integer) constValue;
			}
			else if (constValue instanceof Character && currentType.getKind() == Struct.Char) {
				adr = (int) ((Character) (constValue));
			}
			else if (constValue instanceof Boolean && currentType.getKind() == Struct.Bool) {
					adr =  1;
			} else {
				log.error("Vrednost " + constValue + " nije komaptibilna sa tipom konstante, linija: " + constNameleft);
				return;
			}
			
			globalConstants++;
			Tab.insert(Obj.Var, constName, currentType).setAdr(adr);
			
			
			
			log.info("Deklarisana je konstanta " + constName + " - linija: " + constNameleft);
		} else {
			log.error("Konstanta " + constName + " je vec definisana - linija: " + constNameleft);
		}
	}
	
	
	
	public Obj checkIfExists(String name, int nameleft){
		Obj obj = Tab.find(name);
		if(obj == Tab.noObj){
			log.error("Promenljiva " + name + " nije definisana - linija: " + nameleft);
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
			log.error("Operand instrukcije READ mora bili int, char ili bool linija - " + line);
		}
	}
	
	public void print(Object e, int line){
		if (e != Tab.intType && e != Tab.charType){
	  		log.error("Operand instruckije PRINT mora biti int ili char linija - " + line);
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
	  		log.error("Operand instruckije PRINT mora biti int ili char linija - " + line);
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
	
	
	
	
	
	
	public Struct factorInsertDesignator(Object des){
		Obj d = (Obj) des;
		Struct ret = null;
		Code.load(d);
		if(d == Tab.noObj){
			return Tab.noType;
		} else {
			ret = d.getType();
			return ret;
		}
	}
	
	public Struct factorInsertFunc(Object des, int line){
		Obj d = (Obj) des;
		if(Obj.Meth == d.getKind()){
			if(d.getType() != Tab.noType){
				int adr = d.getAdr() - Code.pc;
				Code.put(Code.call);
				Code.put2(adr);
			}
			return d.getType();
		} else {
			log.error("Funkcija " + d.getName() + " nije definisana linija - " + line);
			return Tab.noType;
		}
	}
	
	
	
	public Struct factorInsertNum(Integer num){
		Obj o = Tab.insert(Obj.Con, "", Tab.intType);
		o.setAdr(num.intValue());
		Code.load(o);
		return Tab.intType;		
	}
	
	public Struct factorInsertChar(Character chr){
		Obj o = Tab.insert(Obj.Con, "", Tab.charType);
		o.setAdr(chr.charValue());
		Code.load(o);
		return Tab.charType;
	}

	public Struct factorInsertBool(Boolean bool){
		Obj o = Tab.insert(Obj.Con, "", new Struct(Struct.Bool));
		o.setAdr(bool.booleanValue() ? 1:0);
		Code.load(o);
		return new Struct(Struct.Bool);
	}
	
	
	
	
	
	
	
	public void increment (Object des, int line){
		Obj d = (Obj) des;
		if (d.getType() == Tab.intType){
			if(d.getKind() == Obj.Elem){
				Code.put(Code.dup2);
			}
			Code.load(d);
			Code.loadConst(1);
			Code.put(Code.add);
			Code.store(d);
			
		} else {
			log.error("Identifikator mora biti tipa int linija - " + line);
		}
	}
	
	public void decrement (Object des, int line){
		Obj d = (Obj) des;
		if (d.getType() == Tab.intType){
			
			Code.load(d);
			Code.loadConst(1);
			Code.put(Code.sub);
			Code.store(d);
			
		} else {
			log.error("Identifikator mora biti tipa int linija - " + line);
		}
	}

}
