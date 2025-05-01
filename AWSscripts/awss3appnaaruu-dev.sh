DRY="--delete --dryrun" 
if [ "$1" = "--go" ]; then DRY=; fi
if [ "$1" = "--delete" ]; then DRY=--delete; fi
aws --profile prod s3 sync s3://cds.dwo.nl/apps/ s3://test-dwo-nl/uu-dev/apps/ --acl public-read $DRY --exclude "plantyn*" --exclude "test*" --exclude "noordhoff/*" --exclude "numworx-*" --exclude "en/*" --exclude "gr/*" --exclude "new/*" --exclude "mw/*" --exclude "negatief/*" --exclude "depp/*" --exclude "player*" --exclude "rot5/*" --exclude "side30/*"
aws --profile prod s3 sync s3://cds.dwo.nl/jars/ s3://test-dwo-nl/uu-dev/jars/ --acl public-read $DRY
aws --profile prod s3 sync s3://cds.dwo.nl/resources s3://test-dwo-nl/uu-dev/resources/ --acl public-read $DRY
