public void ServerMethod(String message) {
  String[] arr = message.split(","); //android studio client sends all the variables we need (except for DATETIME as a string separated by commas. //<>//
  mes = arr[0];
  if (mes.equals("FIRE")) { //During FIRE special case, all doors turn green indefinitely
    col_main = g;
    col_kitchen = g;
    col_office_1 = g;
    col_office_2 = g;
    col_big_office = g;
    col_bathroom_1 = g;
    col_bathroom_2 = g;
    col_conference =g;
    androidInfo("FIRE", "", false, "", ""); //DoorToAccess variable is not relevant for this special case. DATETIME variable will still be calculated in androidInfo
  } else if (mes.equals("INTRUDER")) { //During INTRUDER special case, all doors turn blue indefinitely
    col_main = b;
    col_kitchen = b;
    col_office_1 = b;
    col_office_2 = b;
    col_big_office = b;
    col_bathroom_1 = b;
    col_bathroom_2 = b;
    col_conference =b;
    androidInfo("INTRUDER", "", false, "", ""); //DoorToAccess variable is not relevant for this special case. DATETIME variable will still be calculated in androidInfo
  } else if (mes.equals("MAP")) { //During MAP Special case, no door change but message is sent to android info to be parsed //<>//
    androidInfo("MAP", "", false, "", ""); //DoorToAccess variable is not relevant for this special case. DATETIME variable will still be calculated in androidInfo
  } else {
    try{
    androidInfo(arr[0], arr[1], parseBoolean(arr[2]), arr[3], arr[4]); //DATETIME IS CALCULATED within androidInfo
    }catch(Exception e){
      System.out.println(e);
      System.out.println("Invalid code. Please get your NFC card checked out by IT");
    }
  }
}
