DRY=--dryrun
if [ "$1" = "--go" ]; then DRY=; fi
aws --profile prod s3 sync s3://cds.dwo.nl/apps/ s3://cds-s3-backup/apps/ $DRY --delete
aws --profile prod s3 sync s3://cds.dwo.nl/jars/ s3://cds-s3-backup/jars/ $DRY --delete
