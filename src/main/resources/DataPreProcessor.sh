#!/usr/bin/env bash

# ===
# Script to pre-process ncdc dataset from 'amazon public datasets'.
#
# pre-process includes concatinating smaller files into larger files named by
# the 'year.data' format and dump the merged file into hdfs
#
# Author: Ashrith Mekala
# ===

DATALOC="/ncdc"
MLOC="/tmp"
HDFS_DIR="/ncdcdata"

if [ "$(id -u)" != "0" ]; then
   echo "[ERROR]: This script must be run as root"
   exit 2
fi

if [ ! -d $DATALOC ]; then
  echo "[FATAL]: cannot find the data files path"
  exit 2
else
  if [ ! -d ${DATALOC}/gsod ]; then
    echo "[FATAL]: cannot locate dir 'gsod' in the data path '${DATALOC}'"
    exit 2
  fi
fi

if [ ! -d $MLOC ]; then
  echo "[FATAL]: cannot find the merged data files path"
  exit 2
else
  # clear existing contents
  echo "[DEBUG]: clearing exiting contents"
  rm -rf ${MLOC}/*
fi

command -v hadoop > /dev/null || {
  echo "[FATAL]: hadoop command not found";
  exit 2;
}

function process_dir() {
  local dir_path=$1
  local dir_name=$(basename $dir)
  local MFILE=${MLOC}/${dir_name}.data

  echo "Merging all files in '${dir_path}' into single file named: '${MFILE}'"
  touch $MFILE
  # Concat all files specific to a year to single file
  for file in $(find "${dir_path}" -type f -exec echo "{}" \; | sort); do
    # ignore the first line of the file which is the file header
    tail -n +2 $file >> $MFILE
  done
  echo "putting file ${MFILE} to hdfs://${HDFS_DIR}"
  sudo -u hdfs hadoop fs -put ${MFILE} ${HDFS_DIR}
  rm ${MFILE}
}

echo "Initializing data dump"

# Find all the data directories with year as the directory name
sudo -u hdfs hadoop fs -mkdir ${HDFS_DIR} || { echo "[FATAL]: Failed Creating hdfs path"; exit 2; }

for dir in $(find "${DATALOC}/gsod" -type d -regex ".*/gsod/[0-9][0-9][0-9][0-9]" -exec echo "{}" \; | sort); do
  process_dir $dir
done
