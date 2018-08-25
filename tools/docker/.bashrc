#!/bin/bash
export CLASSPATH=.
## Colorize the ls output ##
alias ls='ls --color=auto'


## Prompt
# 30m - Black
# 31m - Red
# 32m - Green
# 33m - Yellow
# 34m - Blue
# 35m - Purple
# 36m - Cyan
# 37m - White
#   0 - Normal
#   1 - Bold
function prompt {
  local BLACK="\[\033[0;30m\]"
  local BLACKBOLD="\[\033[1;30m\]"
  local RED="\[\033[0;31m\]"
  local REDBOLD="\[\033[1;31m\]"
  local GREEN="\[\033[0;32m\]"
  local GREENBOLD="\[\033[1;32m\]"
  local YELLOW="\[\033[0;33m\]"
  local YELLOWBOLD="\[\033[1;33m\]"
  local BLUE="\[\033[0;34m\]"
  local BLUEBOLD="\[\033[1;34m\]"
  local PURPLE="\[\033[0;35m\]"
  local PURPLEBOLD="\[\033[1;35m\]"
  local CYAN="\[\033[0;36m\]"
  local CYANBOLD="\[\033[1;36m\]"
  local WHITE="\[\033[0;37m\]"
  local WHITEBOLD="\[\033[1;37m\]"
  local NORMAL="\[\033[0;0m\]"
#  local USER="if [ `id -u` == "0" ]; then echo \"${RED}\"; else echo \"${CYAN}\"; fi"
  local SMILEY="${GREEN}:)${NORMAL}"
  local FROWNY="${RED}:(${NORMAL}"
  local SELECT="if [ $? = 0 ]; then echo \"${SMILEY}\"; else echo \"${FROWNY}\"; fi"
  local SELECT2="${SELECT}"
export PS1="${NORMAL}[${CYAN}\u${NORMAL}@${YELLOW}\h${NORMAL}]  ${WHITEBOLD}\w${NORMAL}  \n\\$ "
}

## Use a long listing format ##
alias ll='ls -la'

## Show hidden files ##
alias l.='ls -d .* --color=auto'

## Initialize & validate sedona dev (unix) environment
source ~/sedonadev/adm/unix/init.sh

## Set command prompt
#PS1='\h:\W \u\$ '
clear
w
prompt;
