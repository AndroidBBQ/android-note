## 二进制插件

二进制插件就是实现了 org.gradle.api.Plugin接口的插件，它们可以有plugin id，

```groovy
apply plugin:'java'  //导入方式一
```

这样就把 java 插件运用到项目中了， 'java' 是java插件的 plugin id，是唯一的。java 对应的类型是 org.gradle.api.plugins.JavaPlugin。也可以通过下面的方式引用

```groovy
apply plugin:org.gradle.api.plugins.JavaPlugin  //org.gradle.api.plugins包名是默认导入的。也可以写成下面的样子  导入方式二
apply plugin:JavaPlugin
```

第一种写法用的比较多，容易记，第二种用于 build 文件中自定义插件。

## 应用脚本插件

比如 

version.gradle

```groovy
ext{
	versionName = 'kotlin'
	versionCode = '1.2.1'
}
```

build.gradle

```groovy
apply from:'version.gradle'  //引用 version.gradle

task test << {
	println "versionName : ${versionName}  versionCode : ${versionCode}"
}

//运行 
gradle test
//运行结果
versionName : kotlin versionCode : 1.2.1
```

from 关键字，后面可以跟一个脚本文件，可以是本地的，也可以是网络存在的，如果是网络的话需要使用 http url



## apply的用法

```groovy
//apply的三种使用方法
void apply(Map<String,?> options);
void apply(Closure closure);
void apply(Action<? super ObjectConfigurationAction> action);
```

具体的使用方式

```groovy
//第一种方式
apply plugin:java
//第二种方式
apply{
	plugin 'java'
}
//第三种方式
apply(new Action<ObjectConfigurationAction>(){
	@Override
	void execute(ObjectConfigurationAction objectConfigurationAction){
		objectConfigurationAction.plugin('java')
	}
})
```

## 应用第三方发布的插件

第三方发布的作为jar的二进制插件，我们在应用的时候，必须要先在 buildscript{} 里配置其classpath 才能使用。我们 Android Gradle 插件，就属于 Android 发布的第三方插件。

```groovy
buildscript {
    repositories{
        jcenter()
    }
    dependencies{
        classpath 'com.android.tools.build:gradle:1.5.0'
    }
}
```

buildscript{} 块是一个在构建项目之前，为项目前期准备和初始化相关配置依赖的地方，配置好所需的依赖，就可以应用插件了。

```
apply plugin:'com.android.application'
```

如果没有提前配置好 buildscript 里的 classpath 会提示找不到插件



## plugins DSL 应用插件

2.1 以上的写法

```
plugins {
	id 'java'
}
```

应用第三方插件的时候要先使用 buildscript 配置，但是使用 plugins 有一种例外，如果该插件已经被托管到了 https://plugins.gradle.org/ 网站上，就不用再 buildscript 里配置classpath 依赖了。

```groovy
plugins {
	id 'org.sonarqube' version '1.2'
}
```

