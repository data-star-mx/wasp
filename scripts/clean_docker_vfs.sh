#!/bin/bash

###############################
########### Code ############## 
###############################

clear
echo " *** Before running this script you must be absolutely certain that the path you are providing is correct. ***"
echo " *** We decline every responsibility for any damages to your filesystem. ***"

read -p "Do you want that the script locates automatically the best candidates to be removed? [y/n]: `echo $'\n> '`" choice
choice=${choice:-n}

if [ $choice = "y" ]; then
  echo "Our built-in search procedure is locating possible candidates to be removed (may require a few minutes, based on your machine)... "
  echo "Located candidates : " 
  $(find / ! -readable -prune -type d -name 'vfs')

  read -p "Please enter the location of docker folder [vfs]: `echo $'\n> '`" datadir
  datadir=${datadir:-/home/docker_staging/docker/vfs/}

  echo "Removing all virtual FS created by docker compose ... : $datadir"
  sudo rm * $datadir -rf

fi


if [ $choice = "n" ]; then
  read -p "Please enter the location of docker's folder [vfs]: `echo $'\n> '`" datadir
  datadir=${datadir:-/home/docker_staging/docker/vfs/}

  echo "Removing all virtual FS created by docker compose ... : $datadir"
  sudo rm * $datadir -rf

fi

echo "Exiting!"

