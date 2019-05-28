
## settings
settings.gradle 用于初始化已经工程树的配置，放在根工程目录下。
Gradle的项目很多是通过工程数表示的，比如 Android Studio创建的Android项目中的Project就是根工程，很多module就是子工程，一个根工程可以有很多子工程。
一个子工程只有在Settings文件中配置了之后Gradle才会被识别。

比如 AS 新建 GradleTest 项目，它的 settings.gradle文件中
```
include ':app' //会在settings.gradle 同级中找 app 目录
```

比如书中给出的
```
include ':example2'
project(':example').projectDir = new File(rootDir,'chapter01/example2')
```
如果不指定目录会默认在会在settings.gradle 同级中找 example2 的目录。修改后就会在 同级的 chapter01目录下找 example2 的目录。

实际项目中的
```
include ':app', ':library:authentication-release'
```
会先找到 app 目录，然后在当前目录的 library目录下找 authentication-release目录


```
task customTask {
	doFirst{
		println "customTask:doFirst"
	}
	doLast{
		println "customTask:doLast"
	}
}
```
上面这样写的含义：Task看起来像一个关键字，其实它是 Project 对象的一个函数，原型是 create(String name,Closure configureClosure)。 customTask 是任务的名字，我们可以自定义。第二个参数是一个闭包，也就是花括号中的代码块。由于groovy函数的最后一个参数可以放外面，而且括号可以省略，所以就成了上面的写法。


任务之间是有依赖关系的 dependsOn 可以指定其依赖的任务
```
task hello << {
	println "hello"
}
task world << {
	println "world"
}
task main1(dependsOn: hello){//依赖一个
	println "main1"
}
task main2(){
	dependsOn hello,world  //依赖多个
    println "main2"
}
```


Project 和 Task 都允许用户添加额外的自定义属性，可以通过ext属性来完成。添加一个或多个和使用的代码如下。
```
ext.age = 18 //自定义一个Project的属性

//通过代码块同时自定义多个属性
ext{
	phone = 123
    address = "xxx"
}

在builde中使用
${name} //和groovy中调用变量的方式一样
```
相比较局部变量，自定义属性的范围可以跨Project,跨Task访问。自定义属性还可以应用在 SourceSet 中。
```
sourceSets.all{
	ext.resourcesDir = null   //自定义属性
}
sourceSets{
	main{
    	resourcesDir = "main/res"
    }
    test{
    	resourcesDir = "test/res"
    }
}

//使用
sourceSets.each {
	println "${it.name} 的 resourcesDir 是 ${it.resourcesDir}"
}
```
自定义属性经常运用在项目中的自定义版本号和版本名称。



在脚本中也可以使用代码，在代码中也可以使用脚本,例子
```
def buildTime{
	def date=new Date()
    def formattedDate = date.format('yyyyMMdd')
    return formattedDate
}
```













































