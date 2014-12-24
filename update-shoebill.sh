if [ -f shoebill-updater.jar ];
then
   java -jar shoebill-updater.jar
else
   echo "shoebill-updater.jar does not exists!"
fi
read