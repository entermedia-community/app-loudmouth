#!/bin/sh
##Upgrades this open edit installation
export PATH=$PATH:$JAVA_HOME/bin


if [ $1 ]; then
  case "$1" in
    'Edit')
      	MODULE="editor"
    	;;
    'CartModule')
      	MODULE="cart"
      	;;
    *)
        MODULE=$1
        ;;
  esac
    wget -O install-$MODULE.xml http://dev.openedit.org/anthill/projects/openedit-$MODULE/install-$MODULE.xml
      echo "Upgrade logs can be read from here: ../upgradelog.html" 
      echo "Upgrading $MODULE module...";
      ant -f install-$MODULE.xml > ../upgradelog.html
else
        echo Usage: ./upgrade-module.sh NAMEOFMODULE
        echo Usage: valid values are Edit, CartModule
fi
