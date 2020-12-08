## 前端传递的Json值中的Boolean值无法被正确接收

在学习swagger的过程中，我随便写了几个最简单的接口，其中有一个接口需要接收前端转递过来的Json值，我用自己写的一个POJO对象来接收时，发现没法接收到传递过来的Boolean值。

~~~java
@PostMapping("/add")
    public String add(@RequestBody User user) {
        StringBuilder builder = new StringBuilder("");
        return builder.append("username:").append(user.getUsername()).append(",")
                .append("age: ").append(user.getAge()).append(",")
                .append("isFemale: ").append(user.getIsFemale()).append(",")
                .append("height: ").append(user.getHeight()).toString();
    }
~~~



~~~java
public class User {
    private String username;
    private Integer age;
    private Boolean isFemale;
    private Double height;
~~~

我是用IDEA自带的getter和setter来生成get和set方法，我发现对于属性**isFemale**生成的get/set方法是**getFemale**和**setFemale**形式的。所以对于传递的Josn值调用set方法进行赋值的时候它不能将值正确地赋值到对应的属性上，进行修改之后（修改为**setIsFemale**和**getIsFemale**）就可以正确接收了。

> PS: 如果使用lombok，使用isXXX形式的属性名是可以正确接收值的。

另外，我发现对于属性名中不带 ***is***  的Boolean类型属性，IDEA生成的对应的get/set方法是也是getXXX和setXXX类型的，这就表示IDEA对于Boolean类型的属性的属性名不管是带不带is都是生成getXXX和setXXX。

所以，只用Boolean类型的属性，在命名的时候要格外小心。

附一段阿里巴巴开发手册中的说明：

> 【强制】POJO 类中布尔类型的变量，都不要加 is，否则部分框架解析会引起序列化错误。反例：定义为基本数据类型 Boolean isDeleted；的属性，它的方法也是isDeleted()，RPC框架在反向解析的时候，“以为”对应的属性名称是deleted，导致属性获取不到，进而抛出异常。