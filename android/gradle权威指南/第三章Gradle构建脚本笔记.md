
## 1.settings.gradle
settings.gradle 用于初始化已有工程树的配置，放在根工程目录下。
Gradle的项目很多是通过工程数表示的，比如 Android Studio创建的Android项目中的Project就是根工程，很多module就是子工程，一个根工程可以有很多子工程。一个子工程只有在Settings文件中配置了之后Gradle才会被识别。

比如 AS 新建 GradleTest 项目，它的 settings.gradle文件中
```
include ':app' //会在settings.gradle 同级中找 app 目录
```

比如书中给出的
```
include ':example2'
project(':example').projectDir = new File(rootDir,'chapter01/example2')
```
如果不指定目录会默认在会在settings.gradle 同级中找 example2 的目录。修改后就会在 根目录的chapter01目录下找 example2 的目录。

实际项目中的
```
include ':app', ':library:authentication-release'
```
在根目录下找到 library目录，然后在library目录下找 authentication-release目录

## 2.build文件
  每个 Project 都有一个 build文件，它是该project的入口。对于 root project可以获取到所有的 child project ，所以在 root project 的build文件中可以对child project进行统一配置。
```groovy
allprojects {
	repositories{
		jcenter()
	}
}
```
将所有的child project的仓库为 jcenter.
## 3.创建task
```
task customTask {
	doFirst{//在task之前执行
		println "customTask:doFirst"
	}
	doLast{//在task之后执行
		println "customTask:doLast"
	}
}
```
上面这样写的含义：Task看起来像一个关键字，其实它是 Project 对象的一个函数，原型是 create(String name,Closure configureClosure)。 customTask 是任务的名字，我们可以自定义。第二个参数是一个闭包，也就是花括号中的代码块。由于groovy函数的最后一个参数可以放外面，而且括号可以省略，所以就成了上面的写法。

## 4.任务依赖
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
## 5.任务间通过api控制，交互
比如下面的：
```groovy
task testTask << {
	println "testTask"
}
testTask.doFirst {
	println "dofirst"
}
testTask.doLast {
	println "doLast"
}
```
上面直接使用 testTask 调用 Task 的方法的原理是：Project 在创建该任务的时候，同时把该任务对应的任务名注册为 Project 的一个属性，类型是 Task。
```
project.hasProperty("testTask") //查看Project中是否有 xx 属性
```
## 6.自定义属性
Project 和 Task 都允许用户添加额外的自定义属性，可以通过ext属性来完成。添加一个或多个和使用的代码如下。
```
ext.age = 18 //自定义一个Project的属性

//通过代码块同时自定义多个属性
ext{
	name = "tom"
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
//结果：
main 的 resourcesDir 是 main/res
test 的 resourcesDir 是 test/res
```
自定义属性经常运用在项目中的自定义版本号和版本名称。

## 7.代码和脚本是相互的
在脚本中也可以使用代码，在代码中也可以使用脚本,例子
```
def buildTime{
	def date=new Date()
    def formattedDate = date.format('yyyyMMdd')
    return formattedDate
}
```













































