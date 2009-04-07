#!/bin/sh

echo "Enter your GPG passphrase (input will be hidden)"
stty_orig=`stty -g` 
stty -echo 
read passphrase
stty $stty_orig

# clean all projects
echo "Clean all projects"
mvn clean -Pall

# package and assemble the release
echo "Package and assemble the release"
mvn -ff -Prelease deploy javadoc:javadoc assembly:attached $1

filename=`ls target/dist/apache-empire-db*gz`
gpg --print-md MD5 $filename > $filename.md5
gpg --print-md SHA1 $filename > $filename.sha
echo $passphrase | gpg --passphrase-fd 0 --armor --output $filename.asc --detach-sig $filename

filename=`ls target/dist/apache-empire-db*zip`
gpg --print-md MD5 $filename > $filename.md5
gpg --print-md SHA1 $filename > $filename.sha
echo $passphrase | gpg --passphrase-fd 0 --armor --output $filename.asc --detach-sig $filename
