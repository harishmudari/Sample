###### **Singleton Design Pattern:**

###### 

###### **Creating multiple objects of a class which serves the same functionality is wastage of memory and time.**

######  

###### **Singleton Design Pattern, which allows the application to create only one object of a java**

###### **class and use it for multiple times on each JVM in order to minimize the memory wastage and to increase the**

###### **performance. The class following such design pattern is called singleton java class.**

###### 

###### **Definition: Singleton java class is java class, which allows us to create only one object per JVM.**

###### 

###### **Singleton java class**

###### 

###### **The java class that allows us to create only one objęct per jvm is called singleton javaclass.**

###### **Take singleton java class in the following situations**

###### **a) If class is not having any state**

###### **b) If class is having only sharable read only state (final variable)**

###### **c) If class is having huge amount of state and performing write operations on that state in**

###### **synchronized manner (1 thread at a time)**

###### **Instead of creating multiple objects with same data or no data, create one object and access**

###### **it for multiple times for this take the class as singleton javaclass.**

###### **If we just create one object for java class even through class allows us to create multiple**

###### **objects then that class is not called Singleton java class.**

###### 

###### 

###### **Rules To Develop Singleton Java Class:**

###### **1. Declare a private static reference variable to hold current class Object.This reference will hold null only**

###### **for the first time,after then it will refer to the object forever(till JVM terminates).We will initialize this**

###### **reference using static factory method as discussed in step 3.**

###### **classPrinterUtil {**

###### **privatestaticPrinterUtilinstance=null;**

###### **}**

###### **2. Declare all the constructor as private so that its object cannot be created from outside of the class**

###### **3.**

###### **using new keyword.**

###### **classPrinterUtil {**

###### **privatestaticPrinterUtilinstance=null;**

###### **privatePrinterUtil() {**

###### **System.out.println("PrinterUtil()");**

###### **}**

###### **Develop a static final factory method, which will return a new object only for the first time and the**

###### **same object will be returned then after. Since we have only private constructor, we cannot use new**

###### **keyword from outside of the program, we must declare this method as static, so that it can be**

###### **accessed directly using Class Name. Declare this final so that the child class will have no option to**

###### **override and change the default behavior.**

###### **publicstatic finalPrinterUtilnewInstance(){ if(instance==nul1)**

###### **instance=newPrinterUtil();**

###### **}**

###### **returninstance;**

###### **4. Make Your Singleton class Reflection API proof.**

###### **We know that Reflection API can access the private variables, methods and constructors of the class,**

###### **hence even if your constructor is private, we can still create the object of that class. To prevent this**

###### **declare an instance boolean variable initially holding true. Change its value toö false, immediately when**

###### **constructor is called for the first time.Then after when even the constructor is called for 2nd time,it**

###### **should throw SomeException saying object cannot be created for multiple times.**

###### **This approach also removes the Double Checking Problem in case of Multiple thread trying to create**

###### **object at the same time, which we will discuss later.**

###### 

###### **publicclassPrinterUtil{**

###### **privatestaticbooleanisNew=true;//lst Time-true, 2nd Time-false, Used for Reflection**

###### **API Proof, and Multi-Thread double check**

###### **privatePrinterUtil()**

###### **//To prevent Reflection API creating Multiple Objects**

###### **if(isNew) {**

###### **}**

###### **else {**

###### **isNew=false;**

###### **System.out.println("PrinterUtil()");**

###### **thrownewInstantiationError("Cannot Create Multiple Object");**

###### **}**

###### **5. Make Your factory Method Thread Safety, so that Only one object is created even if more than 1**

###### **thread tries to call this method simultaneously. Declare the whole method as synchronized method, or**

###### **use synchronized block**

###### **{**

###### **publicsynchronizedfinalstaticPrinterUtilgetInstance()**

###### **if(instance==null)**

###### **instance=newPrinterUtil();**

###### **returninstance;**

###### **}**

###### **Naresh IT Mr. Nataraj**

###### **Instead of making the whole factory method as synchronized method, it is good to place only the**

###### **condition check part in synchronized block.**

###### **publicstaticfinalPrinterUtilgetInstance()**

###### **{**

###### **synchronized (PrinterUtil.class){**

###### **if(instance==null){**

###### **instance=newPrinterUtil();**

###### **}**

###### **}**

###### **returninstance;**

###### **}**

###### **we have a problem with the above code, after the first call to the getInstance(), in the next calls to**

###### **the same getinstance() method,the method will check for**

###### **instance == null check, while doing this check, it acquires the lock to verify the condition, which is**

###### **not required. Acquiring and releasing locks are quiet costly**

###### **and we must try to avoid them as much as we can. To solve this problem we can have double level**

###### **checking (2 times null checking) for the condition as shown below.**

###### **publicstaticfinalPrinterUtilgetInstance()**

###### **If(instance==null){ //1st null check**

###### **synchronized (PrinterUtil.class){**

###### **if(instance==null){**

###### **instance=newPrinterUtil(); //2"d null check**

###### **}**

###### **}**

###### **returninstance;**

###### **}**

###### **It is good practice to declare the static member instance as volatile to avoid**

###### **problems in a multi-threaded environment.**

###### **public class PrinterUtil {**

###### **private static volatilePrinterUtil instance;**

###### **}**

###### **Note: If you have used the Reflection Proof logic, then no need to worry about the 2nd null check. Because**

###### **when you call the constructor for 2nd time, it will throw InstantiationError**

###### **Prevent Your Singleton Object from De-serialization. If you need your singleton object to send across**

###### **the network, Your Singleton class must implement Serializable interface.But problem with thisapproach is we can de-serialize it for N number of times,and each deserializationprocess will create a**

###### **brand new object,which will violate the Singleton Design Pattern.**

###### **In order to prevent multiple object creation during deserialization process, override readResolve() and**

###### **return the same object. readResolve() method is called internally in the process of deserialization. It is**

###### **used to replace de-serialized object by your choice.**

###### **publicclassPrinterUtil{**

###### **privatestaticPrinterUtil instance=null;**

###### **privatePrinterUtil() {.... }**

###### **//Any Deserialization Process will give you the same Object**

###### **protected Object readResolve()**

###### **System.out.println("readResolve()");**

###### **return instance;**

###### **7.**

###### **}**

###### **Note: Ignore this process if your class does not implement Serializable interface directly or indirectly.**

###### **Indirectly means the super class or super interfaces has not implemented/extended Serializable**

###### **interface.**

###### 

###### **}**

###### **Note: Ignore this process if your class does not implement Serializable interface directly or indirectly.**

###### 

###### **Indirectly means the super class or super interfaces has not implemented/extended Serializable**

###### **interface.**

###### **Prevent Your singieton Object being Cloning.If your class is direct child of Object class,then I will**

###### **suggest not to implementCloneable Interface, as there is no meaning of cloning the singleton object to**

###### **produce duplicate objects out of it. Both are opposite to each other. However if Your class is the child**

###### **of some other class or interface and that class or interface has implemented/extended Cloneable**

###### **interface, then it is possible that somebody may clone your singleton class thereby creating many**

###### **objects. We must prevent this as well.**

###### **Override clone() in your singleton class and return the same old object. You may also throw**

###### **CloneNotSupportedException.**

###### **publicclassPrinterUtil{**

###### **privatestaticPrinterUtilp-null;**

###### **privatePrinterUtil() {... }**

###### **//Any Cloning Process will return you the Same Old object**

###### **public Object clone() throwsCloneNotSupportedException**

###### 

###### **{**

###### **throw new CloneNotSupportedException();**

###### **//returnp; //If you want to return the same old Object**

###### **Mr. Nataraj**

###### **8.**

###### **9.**

###### **Inspite of All the above efforts, There is still a loop hole "The boss Reflection API".Using reflection API,**

###### **the programmer can get access to private constructors, variables, and methods.We have to prevent**

###### **this for Singleton Design Pattern.**

###### **Declare a static instance variable to count how many times the object .For the first time when ever**

###### **constructor is called, increment the count to 1.Next time when constructor is called, check if the value**

###### **is one or not. If yes, throw Instantiation Error**

###### **Use static-block or static definition. If you feel you don't want to use synchronized method or block but**

###### **still want to achieve singleton behavior. You can use static-block or static definition to initialize the**

###### **singleton java class object as follows.**

###### **publicclassPrinterUtil{**

###### **privatestaticPrinterUtilp-new PrinterUtil();//static definition**

###### **/\* OR**

###### **private static PrinterUtil p=null;**

###### **static{**

###### **p=new PrinterUtil();**

###### **}**

###### **}**

###### **\*/**

###### **privatePrinterUtil()!**

###### **}**

###### **publicfinalstaticPrinterUtilgetInstance ()**

###### **returninstance;**

###### **Note: You have to take care of all the other problems except Multithreading.**

###### **This approach will create the Object even if you don't need them urgently (during class loading).This is**

###### **not used so frequently in the industry.**

###### **Putting it Together Lets see the complete Example**

###### **Naresh IT**

###### **CommonsUtil.java //A super class that has implemented Cloneable, Serializable**

###### **packagecom.nt.commons;**

###### **importjava.io.Serializable;**

###### **publicclassCommonsUtilsimplementsCloneable, Serializable }**

###### **{**

###### **@Override**

###### **p**

###### 

###### 

###### **PrinterUtil.java //Class implementing Singleton Design Pattern**

###### **packagecom.nt.stp;**

###### **importjava.io.Serializable;**

###### **importcom.nt.commons.CommonsUtils;**

###### **publicclassPrinterUtil{ //extends CommonsUtils(**

###### **privatestaticPrinterUtilinstance;**

###### **privatestaticbooleanisInstantiated=false;**

###### **/\*static{**

###### **instance=new PrinterUtil();**

###### **}\*/**

###### **privatePrinterUtil()throwsInstantiationException{**

###### **/\* if(isInstantiated==true){**

###### **throw new InstantiationException();**

###### **}**

###### **}**

###### **else{**

###### **isInstantiated=true;**

###### **}\*/**

###### **System.out.printin("PrinterUtil:0-param cosntructor");**

###### **//no task**

###### **publicstaticPrinterUtilgetInstance(){**

###### **try{**

###### **//if(instance==null){**

###### **synchronized(PrinterUtil.class){ if(instance==null){**

###### **instance=newPrinterUtil();**

###### **}**

###### **}//synchronized**

###### **//}}**

###### **catch(Exception e){**

###### **e.printStackTracе();**

###### **}**

###### **returninstance;**

###### **}**

###### **@Override**

###### **public Object clone() throwsCloneNotSupportedException**

###### **thrownewCloneNotSupportedException();**

###### **}**

###### **public Object readResolve({**

###### **System.out.println("PrinterUtil:readResolve()");**

###### **returninstance;**

###### **}**

###### **/\* public static PrinterUtilgetInstance(){**

###### **return instance;**

###### **}\*/**

###### **}**

###### **{**

###### **SingletonTest.java (Basic test)**

###### **package test;**

###### **importcom.nt.stp.PrinterUtil;**

###### **publicclassSingletonTest {**

###### **publicstaticvoid main(String args\[]) throws Exception{**

###### **PrinterUtil pu1=nuil,pu2=null;**

###### **pul=PrinterUtil.getInstancе();**

###### **pu2=PrinterUtil.getInstancе();**

###### **System.out.printin(pu1.hashCode()+" "+pu2.hashCode());**

###### **System.out.println("pul and pu2 are refering same obj?"+(pu1==pu2));**

###### **}**

###### **}**

###### 

###### **MultiThreadsingletonTest.java**

###### **package test;**

###### **importcom.nt.stp.PrinterUtil;**

###### **classTicketPrinterServletimplements Runnable{**

###### **@Override**

###### **publicvoid run() {**

###### **PrinterUtilpu=null;**

###### **pu=PrinterUtil.getInstance();**

###### **System.out.println("CureentThead name"+Thread.currentThread().getName());**

###### 

###### **System.out.println("PrinteerUtilHashCode"+pu.hashCode()); }**

###### 

###### **publicclassSingletonMultiThreadTester {**

###### **publicstaticvoid main(String\[] args) {**

###### **TicketPrinterServlet servlet=null;**

###### **Thread req1=null;**

###### **Thread req2=null;**

###### **servlet=newTicketPrinterServlet();**

###### **req1=newThread (servlet); req2=newThread(servlet);**

###### **req1.start();**

###### **req2.start();**

###### **}**

###### **}**

###### 

###### 

###### **SingletonCloneTest.java**

###### **package test;**

###### **importcom.nt.stp.PrinterUtil;**

###### **publicclassSingletonCloneTest**

###### **{**

###### **publicstaticvoid main(String\[] args)**

###### **{**

###### **PrinterUtilpu=null,pu1=null;**

###### **// get obi**

###### **pu=PrinterUtil.getInstance();**

###### **//create obj using cloning**

###### **try{**

###### **pu1=(PrinterUtil)pu.clone()3**

###### **System.out.println("puhashCode"+pu.hashCode()+**

###### **"pu1 hashcode"+pu1.hashCode()); }**

###### **catch (Exception e){**

###### **e.printStackTrace();**

###### **}**

###### **}**

###### **}**

###### 

###### 

###### 

###### **package test;**

###### **importjava.io.FileInputStream;**

###### **importjava.io.FileOutputStream;**

###### **importjava.io.ObjectInputStream;**

###### **importjava.io.ObjectOutputStream;**

###### **importcom.nt.stp.PrinterUtil;**

###### **public class SingletonDeSerializationTest }**

###### **public static void main(String\[] args) {**

###### 

###### 

###### **PrinterUtil pul=null,pu2=null;**

###### **ObjectOutputStreamoos=null;**

###### **ObjectInputStreamois=null;**

###### **try{**

###### **Naresh IT**

###### **//perform Serialization on PrinterUtil class obj**

###### **pu1=PrinterUtil.getInstance();**

###### **System.out.println("pu1 hashcode:"+pu1.hashCode());**

###### **oos=new ObjectOutputStream(newFileOutputStream("D:/singleton.ser")); oos.writeObject(pu1);**

###### **System.out.println("Serialization Perfomed");**

###### **ois=new**

###### **}**

###### **catch(Exception e){**

###### **e.printStackTrace();**

###### **}**

###### **//Perform DeSerialization**

###### **try{**

###### **ObjectInputStream(new FileInputStream("d:/singleton.ser"));**

###### **pu2=(PrinterUtil)ois.readobject();**

###### **System.out.printin("DeSerialization Completed");**

###### **System.out.println("pu2 hashCode"+pu2.hashCode());**

###### **}**

###### **catch(Exception e){**

###### **e.printStackTrace();**

###### **}**

###### **}//main**

###### **}//class**

###### 

###### **====================================================================SingletonReflectionTest.java**

###### **package test;**

###### **importjava.lang.reflect.Constructor;**

###### **importcom.nt.stp.PrinterUtil;**

###### **public class ReflectionSingletonTest {**

###### **public static void main(String\[] args) {**

###### **Classclazz=null;**

###### **Constructor cons\[]=null;**

###### **PrintWriterpu=null, pul=null;**

###### **try{**

###### **//Load the class**

###### **clazz =Class.forName("com.nt.stp.PrinterUtil");**

###### **//all get all declared Constructors**

###### **Cons\[]=clazz.getDeclaredConstructors();**

###### **// provide access to Prive constrictor**

###### **cons\[0].setAccessible(true);**

###### **//create obj using above accessed constrictor**

###### **pu=(PrinterUtil)cons\[].newInstance(null);**

###### **System.out.println("puhashcode "+pu.hashCode());**

###### **s Naresh IT**

###### **pul=PrinterUtil.getInstance();**

###### **System.out.println("pu1 hashcode "+pu1.hashCode());**

###### **}**

###### **catch(Exception e ){**

###### **e.printStackTrace();**

###### **}**

###### **}**

###### **}**

###### **===================================================================================================**

###### 

###### 

###### 



