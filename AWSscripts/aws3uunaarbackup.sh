DRY=--dryrun
if [ "$1" = "--go" ]; then DRY=; fi
aws --profile prod s3 sync s3://cds.dwo.nl/uu/ s3://cds-s3-backup/uu/ $DRY --delete
