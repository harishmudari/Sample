###### **Page Object Model : In Selenium, POM stands for Page Object Model,It is a design pattern in Selenium, that creates an object repository for structured and maintainable way of organizing and interacting with web elements, Each web page a web application is represented as a separate class, known as a Page Object.**

###### **This approach promotes code reusability, readability, and easier maintenance.**

###### 

###### **Page Factory is an extension of POM in Selenium that uses annotations like @FindBy to initialize web elements at runtime, simplifying the process of object creation and improving test readability**

###### 

###### 

###### **What is Page Object Model in Selenium?**

###### 

###### **Page Object Model (POM) is a design pattern in Selenium that creates an object repository to store all web elements of an application. It reduces code duplication and simplifies test case maintenance by organizing elements in separate classes.**

###### 

###### **In POM, each web page of the application is represented as a class file. these class files contain only the web elements specific to their corresponding pages.**

###### 

###### **Encapsulation:**

###### **These Page Object classes encapsulate the web elements (e.g., text boxes, buttons, links) and the actions that can be performed on them (e.g., clicking, typing text).**

###### 

###### **What is Page Factory in Selenium?**

###### 

###### **Page Factory is a class provided by Selenium WebDriver from the package called (org.openqa.selenium.support.PageFactory).to support Page Object Design patterns. In Page Factory, we use @FindBy annotation to initialize web elements automatically. which reducing repetitive driver.findElement() calls.**

###### 

###### **PageFactory.initElements(WebDriver driver, Object page);**

###### 

###### **initElements method internally uses java Reflection API to look for @FindBy annotations in the given class.**

###### **This @FindBy annotations reads locator strategy \& value. but It does not immediately find the element because which avoids stale element reference issues if the page is still loading. instead of finding the element immediately, it creates a dynamic proxy object When we interact with the element like click, sendKeys , the proxy will call driver.findElement(By...) at that moment. return the actual WebElement. This approach nothing but lazy initialization, useful for elements that might appear later.**

###### 

###### 

###### **Your short answer:**

###### 

###### **PageFactory helps in implementing POM by initializing WebElements marked with @FindBy, using lazy loading via dynamic proxies. It keeps code cleaner and maintainable. But it doesn’t handle waiting or stale elements, so I combine it with explicit waits in banking UIs that have heavy dynamic content.**

###### 

###### 

###### **Without PageFactory:**

###### **------------------**

###### 

###### **WebElement usernameField = driver.findElement(By.id("username"));**

###### **usernameField.sendKeys("user123");**

###### **--------------------------------------------------------------------**

###### 

###### **With PageFactory:**

###### **----------------**

###### 

###### **@FindBy(id = "username")**

###### **private WebElement usernameField;**

###### 

###### **// In constructor**

###### **PageFactory.initElements(driver, this);**

###### 

###### **// Usage**

###### **usernameField.sendKeys("user123")**

###### **---------------------------------------------------------------------**



###### **Difference between Page Object Model \& Page Factory in Selenium** 

###### 

######        **Page Object Model	                                                         Page Factory**

###### **Finding web elements using By	                                         Finding web elements using @FindBy**

###### **POM does not provide lazy initialization	                         Page Factory does provide lazy initialization**

###### **Page Object Model is a design pattern	                                 PageFactory is a class that implements the Page Object Model design pattern.**

###### **In POM, one needs to initialize every page object individually	         In PageFactory, all page objects are initialized by using the initElements() method**

###### 





**Many senior engineers now prefer plain By locators in a BasePage + custom element getter methods for more flexibility.**









**// Handle SSL warning (if present)**

    **public void handleSSLCertificateWarning() {**

        **try {**

            **wait.until(ExpectedConditions.visibilityOf(advancedButton)).click();**

            **wait.until(ExpectedConditions.visibilityOf(proceedLink)).click();**

            **System.out.println("SSL warning bypassed successfully.");**

        **} catch (TimeoutException e) {**

            **System.out.println("No SSL warning appeared.");**

        **}**

    **}**

