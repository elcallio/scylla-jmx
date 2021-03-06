#!/bin/bash -e

if [ ! -e dist/ubuntu/build_deb.sh ]; then
    echo "run build_deb.sh in top of scylla dir"
    exit 1
fi

if [ -e debian ] || [ -e build ] || [ -e target ] || [ -e m2 ] || [ -e dependency-reduced-pom.xml ]; then
    rm -rf debian build target m2 dependency-reduced-pom.xml
fi

DISTRIBUTION=`lsb_release -i|awk '{print $3}'`
RELEASE=`lsb_release -r|awk '{print $2}'`
CODENAME=`lsb_release -c|awk '{print $2}'`

VERSION=$(./SCYLLA-VERSION-GEN)
SCYLLA_VERSION=$(cat build/SCYLLA-VERSION-FILE | sed 's/\.rc/~rc/')
SCYLLA_RELEASE=$(cat build/SCYLLA-RELEASE-FILE)
if [ "$SCYLLA_VERSION" = "development" ]; then
	SCYLLA_VERSION=0development
fi
echo $VERSION > version
./scripts/git-archive-all --extra version --force-submodules --prefix scylla-jmx ../scylla-jmx_$SCYLLA_VERSION-$SCYLLA_RELEASE.orig.tar.gz 

cp -a dist/ubuntu/debian debian
cp dist/ubuntu/changelog.in debian/changelog
cp dist/ubuntu/rules.in debian/rules
sed -i -e "s/@@VERSION@@/$SCYLLA_VERSION/g" debian/changelog
sed -i -e "s/@@RELEASE@@/$SCYLLA_RELEASE/g" debian/changelog
sed -i -e "s/@@CODENAME@@/$CODENAME/g" debian/changelog
if [ "$RELEASE" = "14.04" ]; then
    sed -i -e "s/@@DH_INSTALLINIT@@/--upstart-only/g" debian/rules
else
    sed -i -e "s/@@DH_INSTALLINIT@@//g" debian/rules
fi
cp dist/common/systemd/scylla-jmx.service.in debian/scylla-jmx.service
sed -i -e "s#@@SYSCONFDIR@@#/etc/default#g" debian/scylla-jmx.service

echo Y | sudo mk-build-deps -i -r
debuild -r fakeroot -us -uc
