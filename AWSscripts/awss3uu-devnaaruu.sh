DRY="--delete --dryrun"
if [ "$1" = "--go" ]; then DRY=; fi
if [ "$1" = "--delete" ]; then DRY=--delete; fi
aws --profile prod s3 sync s3://test-dwo-nl/uu-dev/apps/ s3://cds.dwo.nl/uu/apps/ --acl public-read $DRY
aws --profile prod s3 sync s3://test-dwo-nl/uu-dev/jars/ s3://cds.dwo.nl/uu/jars/ --acl public-read $DRY
aws --profile prod s3 sync s3://test-dwo-nl/uu-dev/resources s3://cds.dwo.nl/uu/resources/ --acl public-read $DRY
