### The Building Blocks of Missions

 eXtensible Markup Language or XML provides for a software and hardware 
 independent way to store and transmit data. Additionally, XML is a 
 language that is designed for coming up with specialized markup 
 languages. For example, one could come up with a language for 
 genealogy, and define tags like ```<mother>```, ```<father>```, 
 ```<son>```, and ```<daughter>```. As a result of this ease of 
 creating new languages, XML has been used in hundreds of document 
 and markup language formats including MathML for specifying 
 mathematical and scientific content, XHTML for extending HTML,
 the popular backbone language of the Internet, and Atom and RSS 
 for delivering updates on regularly changing content. 
   
 Though XML is generally weak syntactically, there is a specification 
 which should be followed for clarity and functionality. A thorough yet 
 brief introduction to the key terminology is given in the 
 <a href="https://en.wikipedia.org/wiki/XML#Key_terminology"> XML 
 Wikipedia page </a>. Given below is an even briefer view on what will 
 be important, especially in the context of Project Malm&ouml;.
 
   - XML comprises of a string of characters. These characters may 
     generally be any 
     <a href="https://en.wikipedia.org/wiki/List_of_Unicode_characters">
     Unicode character</a>.
    
   - The characters making up an XML document are divided into markup 
     and content. These are different using simple syntactic rules.
 
   - As alluded to above in the example given previously, XML at its 
     core is built on tags, like many other Markup Languages. These 
     begin with ```<``` and end with ```>```. More specifically, they come 
     in three flavors:
       * Start-tags like ```<section>```, 
       * End-tags like ```</section>```, and 
       * Empty-element tags like ```<line-break />```.
        
   - A logical document component that either begins with a start-tag 
     and ends with a matching end-tag or consists of only an 
     empty-element tag. The characters between the start- and end-tags, 
     if any are the element's content. Examples are 
     ```<Greeting>Hello, World</Greeting>```, <br/>
     ```<line-break></line-break>``` and 
     ```<line-break />```.
    
   - Attributes are name/value pairs that exist within a start or 
     empty-element tag. In the example below the element _img_ has 
     two attributes, _src_ and _alt_:
     
     ```<img src="proj_malmo.jpg" alt="Project Malmo Image" />```
     
     The above is an empty-element tag. Another example with start- and 
     end-tags, where the only attribute is __number__ with a value of 3,
     is: 
     
     ```<step number="3">Open the XML file.</step>```
     
   - An XML attribute can only have a single value and each attribute 
     can appear only once.
     
   - A processor or XML parser analyzes markup and passes structured 
     information to an application. The specification places 
     requirements on what an XML processor must do or not do, but the 
     application is outside of its scope.
     
Now, the last point about the processor and application is what leads us 
to XSD and JAXB, the next things to get an understanding of Missions in 
Project Malm&ouml;. 

### Rules for the Building Blocks
    
As described in the previous section, XML can be used to come up with 
new markup languages. However, there is a question of how one does this 
and in particular of how the syntax is defined. 
 
XML Schema Definitions or XSD formally describe 
elements in an XML document. Its purpose is to verify each piece of item
content in an XML document, i.e., to parse an XML document using various 
Schemas (rules). 

Without further ado, let's now take a look at the XSD files for Project
Malm&ouml;. Within the Project Malm&ouml; root folder, look for the 
Schemas folder. In there you will find (as of this tutorial's writing 
at least), 5 files with a .xsd extension. These are namely: Mission, 
MissionEnded, MissionHandlers, MissionInit and Types. 

Feel free to go through the files and get an understanding of the 
structure. The general format should be quite intuitive as XML is 
designed to be human readable while still having a well-defined syntax 
like that of many programming languages. 

To highlight a few points about the files and to direct your attention 
to some interesting properties of XSD, given below are somethings you 
can try/have a read through with code snippets as appropriate for 
illustration purposes: 

  - At the top of all of the files, there is the XML declaration which 
    specifies the version of XML to use and the character encoding to 
    use.  
    
  - Just below the XML declaration, is the root element which is 
    required for XSD documents. This root element importantly contains
    information about namespaces which are a common programming language
    concept that prevents name conflicts (eg., when an element is 
    defined in multiple separate schema files).
    
    ```xmlns:xs="http://www.w3.org/2001/XMLSchema"``` indicates that 
    the elements and data types used in the schema come from the w3 
    organizations namespace. It also specifies that elements that come
    from this namespace should be prefixed with xs.
    
    The value of a targetNamespace, 
    ```http://ProjectMalmo.microsoft.com``` is simply a unique 
    identifier, typically a company's project URL, that indicates 
    the elements defined by this schema come from the URL specified.
     
    The default namespace is set to the same URL as the targetNamespace
    using the ```xmlns``` attribute.
    
    Setting elementFormDefault to qualified indicates that any elements
    defined in the Schema must be qualified, i.e, be associated with a 
    namespace, when used in an XML document.
 
  - XML Schema have a lot of built-in data types including:
    <a href="http://w3schools.com/XML/schema_dtypes_string.asp">```xs:string```</a>, 
    <a href="http://w3schools.com/XML/schema_dtypes_numeric.asp">```xs:decimal```</a>, 
    <a href="http://w3schools.com/XML/schema_dtypes_numeric.asp">```xs:integer```</a>, 
    <a href="http://w3schools.com/XML/schema_dtypes_misc.asp">```xs:boolean```</a>, 
    <a href="http://w3schools.com/XML/schema_dtypes_date.asp">```xs:date```</a>, 
    <a href="http://w3schools.com/XML/schema_dtypes_date.asp">```xs:time```</a>.
    
  - A <a href="w3schools.com/xml/schema_simple.asp">Simple Element</a>
    is an XML element that contains text of one of the types included in 
    XSD or it can be a custom type. It cannot contain any other elements
    or attributes. The syntax for defining a simple element is as
    follows: ```<xs:element name="xxx" type="yyy" />```
    
  - An <a href="http://w3schools.com/xml/schema_simple_attribute.asp">
    Attribute</a> is very similar to a simple element. Simple elements
    cannot have attributes. The syntax for defining an attribute is 
    ```<xs:attribute name="xxx" type="yyy" />```
    
  - A <a href="http://www.w3schools.com/xml/el_simpletype.asp">simpleType</a> 
    element defines a simple type and specifies the 
    constraints and information about the values of attributes or 
    text-only elements. For examples, see Types.xsd where enumerations
    are used for definitions of simpleTypes like Colour and BlockType. 
    In particular, note the use of xs:restriction to restrict the values
    the simpleType can take as well as to define the base type which the 
    simpleType takes, such as xs:string.
  
  - A <a href="http://w3schools.com/xml/el_simpletype.asp>complexType</a> 
    element defines an XML element that contains other elements and/or 
    attributes.
  
  - A <a href="http://w3schools.com/XML/schema_complex.asp">Complex Element</a>
    contains other element and/or attributes. There 
    are four types based on the contents: 
    <a href="<a href="http://www.w3schools.com/XML/schema_complex.asp">empty elements</a>, 
    <a href="http://www.w3schools.com/XML/schema_complex_elements.asp">only other elements</a>, 
    <a href="http://w3schools.com/XML/schema_complex_text.asp">only text</a>,
    <a href="http://w3schools.com/XML/schema_complex_mixed.asp">and a mixture of other elements and text</a>. 
    
  - Complex Elements often make use of complexTypes as well as 
    <a href="http://w3schools.com/XML/schema_complex_indicators.asp">
    order and occurence indicators</a>. 
    
    - Order Indicators 
    
        - The ```<all>``` indicator specifies that child elements can 
        appear in any order, and that each child must occur only once.
        - The ```<choice>``` indicator specifies that only one of a list 
        of children elements can be present.
        - The ```<sequence>``` indicator specifies that the child elements
        must appear in a specific order. 
        
    - Occurrence Indicators
    
        - The ```<minOccurs>``` indicator specifies the minimum number of 
        times an element can occur and the ```<maxOccurs>``` indicator
        specifies the maximum number of times an element can occur. 
        
        - Note that the above occurrence indicators can be used to specify 
        the range of the number of times an element can appear. This not
        only is useful to specify the number of times a child element 
        can appear in, say, a sequence, but also for specifying the 
        number of times for example a sequence itself can occur. 
        
  - complexContent defines extensions or restrictions on a complex type 
    that contains mixed content or elements only. 
    
  - xs:group and xs:attributeGroup cannot be extended or restricted in 
    the way xs:complexType or xs:simpleType can. They are purely to 
    group a number of items of data that are always used together.
    
  - Finally, xs:documentation and xs:annotation allow for documentation
    of the XML schema which is one of, if not the most, important parts
    of defining XML schema. Again, if you haven't already, do take a 
    look at the 
    <a href="https://microsoft.github.io/malmo/0.14.0/Schemas/Mission.html">
    Schema documentation</a>.
    
   
