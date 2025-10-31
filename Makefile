##首次编译
build:
	mvn clean install

# 当需要升级版本时，执行该命令
version=1.1.0
update_version:
	mvn versions:set -DnewVersion=${version} versions:commit
	mvn -DskipTests clean install

reset_git:
	git checkout --orphan new_branch
	git add -A -- :!/.idea/ :!/target/ :!/pages/node_modules/ :!/pages/.astro/ :!/pages/.cache/
	git commit -m "Initial commit"
	git branch -D master
	git branch -m master
	git push -f origin master

# 将指定commitId之后的所有提交压缩成一个提交
# 使用方法: make squash_commits commitId=<commitId>
commitId="1805a89ba2692a6085056314fe76efb7f1aa383d"
squash_commits:
	git reset --soft $(commitId)
	git add -A  -- :!/.idea/ :!/target/ :!/pages/node_modules/ :!/pages/.astro/ :!/pages/.cache/
	git commit -m "v1.1.0 开发"
	git push -f origin master
	git push -f github master

