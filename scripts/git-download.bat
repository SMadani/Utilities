REM Warning: this will overwrite local changes!

SET repo=origin
SET branch=master

IF "%~1" NEQ "" ( SET repo=%1 )
IF "%~2" NEQ "" ( SET branch=%2 )

git fetch origin master
git reset —hard FETCH_head
git clean -df
