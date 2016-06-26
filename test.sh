#!/bin/sh
test_url=http://web.$CF_DOMAIN/accounts/123456789

while true; do
	curl -s -o /dev/null -w "%{http_code} " $test_url
done
