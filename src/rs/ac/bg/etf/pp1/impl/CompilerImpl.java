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
	private static Obj leftDesignator;
	private static String currentMethodName;
	private static boolean returned;
	private static Obj currentClass;
	private static Struct currentType;
	private static Struct currentReturnType;
	
	public static boolean ldesignatorIsDereferenced = false;
	public static boolean factorIsNew = false;
	public static boolean mainExists = false;
	public static boolean inForLoop = false;
	
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
		if (mainExists == false) {
			log.error("Program mora da sadrzi main metodu");
		}
		Code.dataSize = Tab.currentScope().getnVars();
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
//			var.setAdr(localAdr);
//			localAdr++;	
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
			Obj con = Tab.insert(Obj.Con, constName, currentType);
			con.setAdr(adr);			
			
		} else {
			log.error("Konstanta " + constName + " je vec definisana - linija: " + constNameleft);
		}
	}

	
	public void insertMethod(Struct retType, String methName, int line){
		returned = false;
		if(Tab.currentScope().findSymbol(methName) != null){
			log.error("Metoda " + methName + " je vec definisana - linija: " + line);
		} else  {
			if (methName.equals("main")) {
				if (retType != null) {
					log.error("Metoda MAIN ne sme imati povratnu vrednost - linija: " + line);
					return;
				} else {
					mainExists = true;
				}
			}
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
				log.error("Vracena vrednost ne odgovara povratnoj vrednosti metode - linija: " + line);
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
	
	public void startMethod() {
		
		currentMethod.setAdr(Code.pc);
		
		if (currentMethodName.equals("main")) {
			Code.mainPc = currentMethod.getAdr();
		}
		
		
		Code.put(Code.enter);
		Code.put(currentMethod.getLevel());
		Code.put(Tab.currentScope().getnVars());

	}
	
	public void endMethod(){
		if (currentReturnType != Tab.noType && !returned) {
			log.error("Metoda " + currentMethod.getName() + " nema RETURN iskaz");
		} else {
			Code.put(Code.exit);
	        Code.put(Code.return_);
	        
			Tab.chainLocalSymbols(currentMethod);
			Tab.closeScope();
			currentMethod = null;
			currentMethodName = null;
			currentReturnType = null;
		}
		
	}
	
	
	
	
	public void insertClass(String className,int classNameleft){ 
		if(Tab.currentScope().findSymbol(className) == null){
			currentClass = Tab.insert(Obj.Type, className, new Struct(Struct.Class)); 
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
	
	
	
	
	
	public Obj findDesignator(String name, Object ref, int nameleft){
		
		Obj obj = Tab.currentScope().findSymbol(name);
		if(obj == Tab.noObj || obj == null){
			obj = universeScope.findSymbol(name);
			if (obj == Tab.noObj || obj == null){
				log.error("Promenljiva " + name + " nije definisana - linija: " + nameleft);
				return Tab.noObj;
			}
		}
		
		if (ref == null && obj.getType().getKind() != Struct.Array) {
			return obj;
		} else if (ref != null && obj.getType().getKind() == Struct.Array){
			return new Obj(Obj.Elem,name,obj.getType().getElemType());
		} else {
			return Tab.noObj;
		}
	}
	
	public Obj findLDesignator(String name,  int nameleft){
		Obj obj = Tab.currentScope().findSymbol(name);
		if(obj == Tab.noObj || obj == null){
			obj = universeScope.findSymbol(name);
			if (obj == Tab.noObj || obj == null){
				log.error("Promenljiva " + name + " nije definisana - linija: " + nameleft);
				return Tab.noObj;
			}
		}
		if (ldesignatorIsDereferenced) {
			if (obj.getType().getKind() != Struct.Array) {
				log.error("Dereferencirana promenljiva nije niz");
				return Tab.noObj;
			}
			return new Obj(Obj.Elem,"",obj.getType().getElemType());
		} else {
			leftDesignator = obj;
			return obj;
		}
	}
	
	public void read(Obj des, int line){
		int type = des.getType().getKind();
		if(type == Struct.Int){
			Code.put(Code.read);
			Code.store(des);
		} else if (type == Struct.Char){
			Code.put(Code.bread);
			Code.store(des);
		} else {
			log.error("Operand instrukcije READ mora biti INT ili CHAR - linija: " + line);
		}
	}
	
	public void print(Obj des, int num, int line){
		int type = des.getType().getKind();
		if (des.getKind() == 5) {
//			Obj obj = new Obj(Obj.Elem, des.getName(), new Struct(Struct.Array, des.getType().getElemType()));
//			Code.load(obj);
		}
	  	if (type == Struct.Int){
	  		Code.loadConst(5);
	  		Code.put(Code.print);
	  	} else if (type == Struct.Char){
	  		Code.loadConst(1);
	  		Code.put(Code.bprint);
	  	} else {
	  		log.error("Operand instruckije PRINT mora biti tipa INT ili CHAR - linija: " + line);
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
		Code.load(o);
		return o;		
	}
	
	public Obj factorInsertChar(Character chr){
		Obj o = new Obj(Obj.Con,"",new Struct(Struct.Char));
		o.setAdr(chr.charValue());
		Code.load(o);
		return o;
	}

	public Obj factorInsertBool(Boolean bool){
		Obj o = new Obj(Obj.Con,"",new Struct(Struct.Bool));
		o.setAdr(bool.booleanValue() ? 1:0);
		Code.load(o);
		return o;
	}
	
	
	
	public void execAssign(String name, Obj expr, Object ref, Integer op, int line) {
		Obj des = findLDesignator(name, line);
		
		checkAssignComaptibility(des, expr, op, line);
		
		if (op == 0) {
			if (ref != null) { // ako je r-value dereferenciran bice postavljen kind na Obj.Elem odn. 5
				des = new Obj(Obj.Elem, des.getName(), new Struct(Struct.Array, des.getType().getElemType()));
				
			} 
//			if (expr.getKind() == 5) {
//				Obj der = new Obj(Obj.Elem, des.getName(), new Struct(Struct.Array, des.getType().getElemType()));
//				Code.load(der);
//			}
			Code.store(des);
		} else {
//			if (expr.getKind() == 5) {
//				Obj der = new Obj(Obj.Elem, des.getName(), new Struct(Struct.Array, des.getType().getElemType()));
//				Code.load(der);
//			}
			Obj right = new Obj(Obj.Var,"",Tab.intType);
			Code.store(right);
			Code.load(des);
			Code.load(right);
			Code.put(op);
			Code.store(des);
		}
		
		ldesignatorIsDereferenced = false;
		factorIsNew = false;
	}

	public Obj execAddopLeft(Obj terms, Obj term, Integer op, int line) {
		Obj o = checkComaptibility(terms, term, op, line);
		if (o != Tab.noObj) {
			Code.put(op);
		}
		return o;
	}
	
	public Obj execMulopLeft(Obj term, Obj factor, Integer op, int line) {
		Obj o = checkComaptibility(term, factor, op, line);
		if (o != Tab.noObj) {
			Code.put(op); 
		}
		return o;
	}

	public Obj execMulopRight(Obj terms, Obj termsWrapper, Integer op, int line) {
		Obj o = checkComaptibility(terms, termsWrapper, op, line);
		if (o != Tab.noObj) {
			Code.put(op);
			Code.store(terms);
			Code.load(terms);
		}
		return o;
	}

	public Obj execAddopRight(Obj terms, Obj termsWrapper, Integer op, int line) {
		Obj o = checkComaptibility(terms, termsWrapper, op, line);
		if (o != Tab.noObj) {
			
			Code.put(op);
			Code.store(terms);
			Code.load(terms);
		}
		return o;
	}
	
	
	public Obj checkComaptibility(Obj first, Obj second, Integer op, int line) {
		
		if (first.getType().getKind() != Struct.Int && op != 0) {
			log.error("Ilegalna operacija - linija: " + line);
			return Tab.noObj;
		}
		if (first.getType().getKind() == second.getType().getKind() && first.getType().getElemType() == second.getType().getElemType()) {
			return first;
		} else {
			log.error("Tipovi nisu kompatibilni - linija: " + line);
			return Tab.noObj;
		}
	}
	
	public void checkAssignComaptibility(Obj left, Obj right, Integer op, int line) {
		if (left.getType().getKind() != Struct.Int && op != Code.eq) {
			log.error("Ilegalna operacija - linija: " + line);
			return;
		}
		
		if (left.getKind() == Obj.Con) {
			log.error("Vrednost se ne moze dodeliti konstanti - linija " + line);
			return;
		}
		
		if (factorIsNew) {
			if (left.getType().getKind() != Struct.Array) {
				log.error("Promenljiva mora biti niz - linija: " + line);
			} else {
				if (left.getType().getElemType().getKind() != right.getType().getKind()) {
					log.error("Tipovi nisu kompatibilni - linija: " + line);
				}
			}
		} else {
			if (left.getType().getKind() != right.getType().getKind()) {
				log.error("Tipovi nisu kompatibilni - linija: " + line);
			}
		}
		
		ldesignatorIsDereferenced = false;
		factorIsNew = false;
	}
	
	public void setLDesignatorReference(String name, Object ref, Integer op) {
		if (ref != null) {
			ldesignatorIsDereferenced = true;
			Obj o = Tab.insert(Obj.Var,"",Tab.intType);
			Code.store(o);
			Obj ob = Tab.currentScope().findSymbol(name);
			if (ob == null) {
				ob = universeScope.findSymbol(name);
			}
			Code.load(ob);
			Code.load(o);
			
			if (op != 0) {
				Code.load(ob);
				Code.load(o);
			}
			
		} else {
			ldesignatorIsDereferenced = false;
		}
	}
	
	public void setArrayOnStack(Obj des) { 
		Obj realDes = Tab.currentScope().findSymbol(des.getName());
		if (realDes == null) {
			realDes = universeScope.findSymbol(des.getName());
		}
		if (realDes != null && realDes.getType().getKind() == Struct.Array) {  // ovo treba raditi samo u slucaju da je niz u pitanju
			Obj o = Tab.insert(Obj.Var,"",Tab.intType);
			Code.store(o);
			Code.load(realDes);
			Code.load(o);
			
			Obj der = new Obj(Obj.Elem, des.getName(), new Struct(Struct.Array, des.getType().getElemType()));
			Code.load(der);
			
		} else {
			Code.load(des); 
		}
		
	}

	public Object compare(Obj left, Obj right, Integer op, int line) {
		if (left.getType().getKind() == right.getType().getKind()) {
			if (op != Code.eq && op != Code.ne && left.getType().getKind() != Struct.Int) {
				log.error("Operandi uporedjivanja za operacije '>, <, >= , <=' moraju biti tipa INT - linija: " + line);
				return Tab.noObj;
			} else {
				return left;
			}
		} else {
			log.error("Operandi uporedjivanja moraju biti istog tipa - linija: " + line);
			return Tab.noObj;
		}
	}
	
	public void checkIfMain() {
		if (currentMethodName.equals("main")) {
			log.error("MAIN metoda ne sme da ima parametre");
		}
	}
	
	public void checkDesignatorInt(Obj des, int line) {
		if (des.getType().getKind() != Struct.Int) {
			log.error("Operand inkrementiranja mora biti tipa INT - linija: " + line);
		}
	}
	
	public void checkDesignatorDec(Obj des, int line) {
		if (des.getType().getKind() != Struct.Int) {
			log.error("Operand dekrementiranja mora biti tipa INT - linija: " + line);
		}
	}
	
	public void checkInForLoopBreak(int line) {
		if (!inForLoop) {
			log.error("BREAK moze da se nalazi samo u FOR petlji - linija: " + line);
		}
	}
	
	public void checkInForLoopContinue(int line) {
		if (!inForLoop) {
			log.error("CONTINUE moze da se nalazi samo u FOR petlji - linija: " + line);
		}
	}

	public Obj checkNewType(Struct type, Obj expr, int line) {
		
		if (expr == null) { // klasa
			if (type != null || type != Tab.noType) {
				return new Obj(Obj.Var, "" , type);
			} else {
				return Tab.noObj;
			}
		} else { // niz
			
			if (expr.getType().getKind() == Struct.Int) {
				factorIsNew = true;
				Code.put(Code.newarray);
				Code.put(1);
				return new Obj(Obj.Var, "" , type);
			} 
			else if (expr.getType().getKind() == Struct.Char) {
				return Tab.noObj;
			} else {
				log.error("Izraz izmedju [ ] mora biti tipa INT - linija: " + line);
				return Tab.noObj;
			}
		}
		
		
	}
	

}
