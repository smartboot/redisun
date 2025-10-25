##首次编译
build:
	mvn clean install

# 当需要升级版本时，执行该命令
version=1.0.0
update_version:
	mvn versions:set -DnewVersion=${version} versions:commit
	mvn -DskipTests clean install

reset_git:
	git checkout --orphan new_branch
	git add -A -- :!/.idea/ :!/target/
	git commit -m "Initial commit"
	git branch -D master
	git branch -m master
	git push -f origin master
	git push -f github master