task的创建
```groovy
//第一种方式
def Task task1=task(task1)
task1.doLast{
	println "创建方法原型  Task task(String name) throws InvalidUserDataException"
}
//第二种方式
def Task task2=task(task2,group:BasePlugin.BUILD_GROUP)
task2.doLast{
	println "创建方法原型为：Task task(Map<String,?> args,String name)"
	println "任务分组 ${task2.group}"
}
//第三种方式
task task3 {
	description '演示任务创建task3'
	doLast{
		println "创建方法原型  Task task(String name,Closure configureClosure)"
		println "任务描述：${description}"
	}
}

//第四种方式 通过TaskContainer的create方法创建
tasks.create("task4"){
	description '演示任务创建task4'
	doLast{
		println "创建方法原型  Task create(String name,Closure configureClosure)"
		println "任务描述：${description}"
	}
}
```



##  访问任务

我们创建的任务都会作为项目的一个属性，属性名就是任务名，所以可以直接通过该任务名称访问和操纵该任务。

```groovy
task task1
task1.doLast{
    println "task1 doLast"
}
```

可以通过访问集合元素的方式访问我们创建的任务

```groovy
task task2
tasks['task2'].doLast{
    println 'task2.doLast'
}
```

tasks的类型是TaskContainer。这里[] 并不是map中的key-value。groovy中对[]进行了重载，a[b]对于的是a.getAt(b)方法，所以上面的 tasks['task2'] 其实是 tasks.getAt('task2') 方法。

通过路径访问任务和通过名字访问路径，都是有两种方式，一种是 get ，另一种是 find，他们的区别在于get的时候如果找不到任务就会抛出 UnknowTaskException 异常，而 find 找不到任务的时候会返回null。

```groovy
//通过路径
tasks.getByPath(':example:task2')
tasks.findByPath(':example:task2')
//通过名称
tasks.getByName('task2')
tasks.findByName('task2')
```



## 任务的分组和描述

任务是可以分组和描述的，分组是对任务的分类，便于对任务进行归类整理，描述是为了说明这个任务有什么作用，创建任务的时候最好进行配置。

```groovy
def Task task1=task(task1)
task1.group = BasePlugin.BUILD_GROUP
task1.description = '这是一个构建引导任务'
task1.doLast{
	println "group:${group} , description:${description}"
}
```

可以通过 `gradle tasks` 查看任务。



<< 在gradle 5.1之后就被废除了



执行排序

```groovy
class CustomTask extends DefaultTask{
	@TaskAction
	def doSelf(){
		println "self"
	}
}
def Task myTask=task myTask(type:CustomTask)

myTask.doFirst{
	println "first"
}

myTask.doLast{
	println "last"
}
```

## 任务的启动和禁用

Task 中有个 enbaled 属性，用于启动和禁止任务，默认是 true ，表示启动，设置为 false，则禁止改人执行，输出会提示改任务被跳过。

```groovy
myTask.enabled=false   //myTask的任务会被跳过
```



## 任务的 onlyIf 断言

断言是一个条件表达式，Task 有一个 onlyIf 方法，它接收一个闭包作为参数，如果该闭包返回 true 则该任务执行，否则跳过。这有很多用途，比如控制什么情况下打包，什么情况下执行单元测试。



```
final String BUILD_APPS="all"
final String BUILD_APPS_SHOUFA="shoufa"
final String BUILD_APPS_EXT_SHOUFA="ext_shoufa"

task baidu << {
	println "打个百度的包"
}

task tenxun << {
	println "打个腾讯的包"
}

//首发是百度 ，额外的是tenxun 
task build {
	group BasePlugin.BUILD_GROUP
	description "打渠道包"
}

build.dependsOn baidu,tenxun

//使用onlyIf 做条件判断
baidu.onlyIf {
	def execute =false
	//如果项目中有 build_apps 的属性
	if(project.hasProperty("build_apps")){
		//获取 build_apps 属性的值
		Object buildApps=project.property("build_apps")
		//如果 build_apps 的值为首发，或者构建所有
		if(BUILD_APPS.equals(buildApps) ||
		BUILD_APPS_SHOUFA.equals(buildApps)){
			execute = true;
		}else{
			execute=false;
		}
	}
	execute
}
tenxun.onlyIf {
	def execute =false
	//如果项目中有 build_apps 的属性
	if(project.hasProperty("build_apps")){
		//获取 build_apps 属性的值
		Object buildApps=project.property("build_apps")
		//如果 build_apps 的值为不是首发的，或者构建所有
		if(BUILD_APPS.equals(buildApps) ||
		BUILD_APPS_EXT_SHOUFA.equals(buildApps)){
			execute = true;
		}else{
			execute=false;
		}
	}
	execute
}

//执行
gradle build 
//一个都没有执行

gradle -Pbuild_apps="all" build
输出： 打个百度的包
	  打个腾讯的包
	 
gradle -Pbuild_apps="shoufa" build
输出：	打个百度的包

gradle -Pbuild_apps="ext_shoufa" build
输出：	打个腾讯的包
```















































