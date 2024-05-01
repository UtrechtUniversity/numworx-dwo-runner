DRY=--dry-run
set -x
if [ "$1" = "--go" ]; then DRY=; fi
if [ "$1" = "--delete" ]; then DRY=--delete-destination=true; fi

export AWS_ACCESS_KEY_ID=key
export AWS_SECRET_ACCESS_KEY=secret
export KEY=azure
. $HOME/aws.env
#EXP=$(date -v+1d +%Y-%m-%d)
echo $EXP
SAS=$(az storage container generate-sas --account-name numworxacc --name prod  --auth-mode key  --permissions dlrw --expiry $EXP --account-key $KEY)
SAS=$(echo $SAS|tr -d '"')
SRC=https://numworxacc.blob.core.windows.net/test
DST=https://numworxacc.blob.core.windows.net/prod

azcopy sync $SRC/apps $DST/apps?"$SAS" $DRY --recursive
azcopy sync $SRC/jars $DST/jars?"$SAS" $DRY --recursive
#azcopy sync $SRC/resources $DST/resources?"$SAS" $DRY --recursive
