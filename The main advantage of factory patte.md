###### **The main advantage of factory pattern is, Which abstracts the object creational process of the classes.**

###### 

###### **This factory pattern handles the object creation by defining a separate method for creating the objects,**

###### **this method optionally accept parameters defining for which class the object should be created, and returns the created object.**



###### **Problem: Creating object by knowing its creational process and dependencies is very complex and Creating**

###### **multiple objects for multiple classes and utilizing one of them based on the user supplied data is wrong**

###### **methodology, because the remaining objects become unnecessarily created objects.**

###### 

###### **Solution: Use Factory Pattern, here the method of class factory instantiates one of the several sub classes**

###### **other classes based on the data that is supplied by user (at runtime), that means objects for remaining sub**

###### **classes/other classes will not be created.**

###### 

###### **Creates objects without exposing the instantiation logic to the client(Provides abstraction on object**

###### **creation process).**

###### **Refers to the newly created object through a common interface.**

