DRY=--dryrun
set -x -e
cd /var/tmp
UU=uu
rm -rf $UU
mkdir $UU
cd $UU
azcopy copy https://numworxacc.blob.core.windows.net/uu-dev/ . --recursive=true
cd uu-dev
aws --profile prod s3 sync apps/ s3://cds.dwo.nl/uu/apps/ --acl public-read --delete $DRY
aws --profile prod s3 sync jars/ s3://cds.dwo.nl/uu/jars/ --acl public-read --delete $DRY
cd ../..
rm -rf $UU
 
