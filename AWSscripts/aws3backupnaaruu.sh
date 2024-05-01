DRY=--dryrun
if [ "$1" = "--go" ]; then DRY=; fi
aws --profile prod s3 sync s3://cds-s3-backup/apps/ s3://cds.dwo.nl/uu/apps/ $DRY --delete --acl public-read  --recursive
aws --profile prod s3 sync s3://cds-s3-backup/jars/ s3://cds.dwo.nl/uu/jars/ $DRY --delete --acl public-read --recursive
