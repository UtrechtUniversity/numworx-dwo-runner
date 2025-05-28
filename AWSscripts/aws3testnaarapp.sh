DRY=--dryrun
if [ "$1" = "--go" ]; then DRY=; fi
if [ "$1" = "--delete" ]; then DRY=--delete; fi
aws --profile prod s3 sync s3://test-dwo-nl/apps/ s3://cds.dwo.nl/apps/ --acl public-read $DRY
aws --profile prod s3 sync s3://test-dwo-nl/jars/ s3://cds.dwo.nl/jars/ --acl public-read $DRY
aws --profile prod s3 sync s3://test-dwo-nl/bundles/ s3://cds.dwo.nl/bundles/ --acl public-read $DRY
