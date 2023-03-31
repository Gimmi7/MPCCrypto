user_num=$1

# 校验输入的user_num
# shellcheck disable=SC2003
expr "$user_num" "+" 10 &>/dev/null
# shellcheck disable=SC2181
if [ $? -ne 0 ]; then
  echo "------please input a number as user_num"
  exit 1
elif [ "$user_num" -lt 1 ]; then
  echo "--------please input a number as user_num >=1"
  exit 1
fi

for i in $(seq 1 "$user_num"); do
  echo "run test $i"
  userid="user_$i"
  mvn test -Dtest=com.unboundTech.mpc.client.SyncClientTest -Dtest.userid="$userid" &> ./concurrent_test"$i".log &
done
