#!/bin/sh
export AWS_ACCESS_KEY_ID=key
export AWS_SECRET_ACCESS_KEY=secret
export KEY=azure
. $HOME/aws.env
echo $EXP
SAS=$(az storage container generate-sas --account-name numworxacc --name test  --auth-mode key  --permissions dlrw --expiry $EXP --account-key $KEY)
SAS=$(echo $SAS|tr -d '"')
SRC=https://s3.eu-west-1.amazonaws.com/test-dwo-nl/
azcopy copy $SRC https://numworxacc.blob.core.windows.net/test/?"$SAS" --recursive=true --overwrite=ifSourceNewer 

