本文得目的是为了记住这些命令和以后可以直接到这里来找相应得命令。如果对于git不了解可以参考 https://www.liaoxuefeng.com/wiki/0013739516305929606dd18361248578c67b8067c8c017b000

安装完成后配置属性
```
git config --global user.name "Your Name"
git config --global user.email "email@example.com"
```
初始化
```
git init
```
添加到仓库
注意：每次修改，如果不用git add到暂存区，那就不会加入到commit中。
```
git add a.txt  //添加一个文件
git add app //添加一个文件夹
git add .  //添加当前所有
```
提交
```language
git commit -m "first commit"
```
查看提交的日志
```language
git log  //全部日志
git log --pretty=oneline  //只显示 hash值和提交信息
```
回退上一个版本
```language
git reset --hard HEAD^   //HEAD是指当前的头所在位置(最后一次提交的位置)  ^代表上一个  ^^代表上上个
```
回归到制定的提交版本
```language
git reset --hard 1094a //版本号没有必要写全，可以自动识别，这个指导的版本号即可以是当前版本之前的，也可以是之后的
```
查看所有的命令历史
```
git reflog
```
查看状态
```language
git status
```
查看工作区和版本库里面最新版本的区别
```language
git diff HEAD -- readme.txt
```
撤销修改
```language
git checkout -- file //修改了文件，还没有添加到暂存区 想撤销修改
git reset HEAD <file>  //修改了文件，已经添加到了暂存区 想撤销修改
如果已经提交了，回退到上个版本
```

删除文件
```language
git rm a.txt  //本地文件删除后，如果远程仓库也要删除这个文件
git checkout -- a.txt //如果本地删除后，后悔了，想要从远程仓库中恢复这个文件
```

创建公匙
```language
ssh-keygen -t rsa -C "gxl@qq.com"
```

关联远程仓库
```language
git remote add origin git@github.com:michaelliao/learngit.git //关联远程仓库，origin可以随便取名
```

推送本地仓库到远程仓库中
```language
//第一次推送
git push -u origin master //把当前分支master推送到远程仓库中，第一次推送master分支时，加上了-u参数，Git不但会把本地的master分支内容推送的远程新的master分支，还会把本地的master分支和远程的master分支关联起来，在以后的推送或者拉取时就可以简化命令。
//以后推送
git push origin master
```

拉取远程仓库中最新数据
```language
 git pull origin master --allow-unrelated-histories //这个是第一次拉取数据，如果发生错误的话用这条命令
 git pull origin master //第一次拉取成功后，后面就用这条命令
```

克隆
```language
git clone git@github.com:michaelliao/gitskills.git
```

创建分支，并切换到分支上
```language
git checkout -b dev //这条命令实际上 checkout 代表着切换到dev分支  -b 代表着创建dev分支
//上面的一条命令，相当于下面的两条命令
git branck dev //创建dev分支
git checkout dev //切换到dev分支上
```

删除分支
```language
git branch -d dev //删除dev分支
```

查看所有分支
```language
git branch //查看所有分支 当前分支上会带有 *
```

合并分支
如果是下面的这样属于快进模式合并，直接将head移动到了dev上
![](https://cdn.liaoxuefeng.com/cdn/files/attachments/001384908892295909f96758654469cad60dc50edfa9abd000/0)
```language
git merge dev //把dev分支的工作成果合并到master分支上 这是属于快速合并
```
如果是下面的这样分支要合并,可能会无法使用快进模式合并，而且可能会有冲突
![](https://cdn.liaoxuefeng.com/cdn/files/attachments/001384909115478645b93e2b5ae4dc78da049a0d1704a41000/0)

解决冲突
>当Git无法自动合并分支时，就必须首先解决冲突。解决冲突后，再提交，合并完成。
使用 `git margin dev `如果有冲突会报冲突，在提示中有readme.txt文件可以查看冲突内容
使用 `git status` 也能查到冲突文件
使用 `cat readme.txt` 可以看到冲突内容 / Git用<<<<<<<，=======，>>>>>>>标记出不同分支的内容
修改 readme.txt中的内容
使用 `git add readme.txt` 将文件添加
使用 `git commit -m "解决冲突"` 将文件提交
解决完成！


查看分支合并图
```language
git log --graph --pretty=oneline --abbrev-commit
```

禁用快速合并模式合并分支
快速合并分支后，删掉原来的分支可能会造成数据流失,禁用后合并图：
![](https://cdn.liaoxuefeng.com/cdn/files/attachments/001384909222841acf964ec9e6a4629a35a7a30588281bb000/0)
```language
git merge --no-ff -m "merge with no-ff" dev //禁止使用快速合并来合并分支
```


Bug分支
修复bug时，我们会通过创建新的bug分支进行修复，然后合并，最后删除bug分支。
这时如果我们手头工作没有完成，但是又不想提交。
可以使用
```
git stash //把当前工作现场“储藏”起来，等以后恢复现场后继续工作
```
创建新bug分支，修改好后，切换到master分支上并将bug分支删除。

查看工作现场
```language
 git stash list //查看工作现场
```
恢复工作现场
```language
//第一种方式
git stash apply  //恢复后，stash内容并不删除，你需要用git stash drop来删除
//第二种方式
git stash pop //恢复的同时把stash内容也删了
```

<br>
删除没有合并过的分支
```language
git branch -D <name>
```
<br>
多人协作

第一次从远程服务器上clone下来项目是没有dev分支的，所以可以新建一个dev分支，如果远程仓库没有dev分支，则从下面第一步开始，如果远程仓库中有dev分支则从第二步：

(1) 可以试图用`git push origin <branch-name>`推送自己的修改；

(2) 如果推送失败，则因为远程分支比你的本地更新，需要先用git pull试图合并；

(3) 如果合并有冲突，则解决冲突，并在本地提交；

(4) 没有冲突或者解决掉冲突后，再用`git push origin <branch-name>`推送就能成功！

(5) 如果git pull提示no tracking information，则说明本地分支和远程分支的链接关系没有创建，用命令`git branch --set-upstream-to <branch-name> origin/<branch-name>`。

<br>
rebase
```language
//rebase操作可以把本地未push的分叉提交历史整理成直线；
//rebase的目的是使得我们在查看历史提交的变化时更容易，因为分叉的提交需要三方对比。
git rebase
```
<br>

新增标签
```language
git tag v1.0
```
查看所有标签
```language
git tag
```
给制定的提交打标签
```language
git tag v0.9 f52c633 //git tag 标签名 提交的sh值
```
查看具体标签的信息
```language
git show v0.9
```
创建带有说明的标签
```language
git tag -a v0.1 -m "version 0.1 released" 1094adb  //-a 制定标签名  -m 说明  
```
删除标签
```language
git tag -d v0.1 //删除v0.1标签
```
推送标签到远程
```language
git push origin v1.0
```
一次推销全部未推销到远程的标签
```language
git push origin --tags
```
删除远程标签
```language
//如果要删除远程标签需要先删除本地标签
 git tag -d v0.9
 //然后远程删除
 git push origin :refs/tags/v0.9
```

<br>
和远程库关联

```java
git remote add origin git@gitee.com:liaoxuefeng/learngit.git
```

查看远程库信息
```java
git remote -v  //查看所有的远程库
```

删除关联的远程库
```java
git remote rm origin
```

<br>
可以将一些忽略文件放到.gitignore根目录的文件夹下

<br>
别名

`git reset HEAD file`可以把暂存区的修改撤销掉（unstage）

为了不每次都写得那么长，我们可以给`reset HEAD`起个别名叫unstage
```java
git config --global alias.unstage 'reset HEAD'
```

这样再进行修改撤销操作时可以
```java
git unstage file //设置完别名后 和git reset HEAD file命令一样
```

如果需要删除别名，可以直接到.git/config文件中进行修改或删除
