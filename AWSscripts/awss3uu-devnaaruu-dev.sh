aws --profile prod s3 sync s3://test-dwo-nl/uu-dev/ s3://cds.dwo.nl/uu-dev/ --acl public-read --delete
