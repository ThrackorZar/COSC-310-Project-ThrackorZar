public void androidInfo(String RN, String DTA, boolean a, String latlng, String t) { //<>//
 if(RN.equals("FIRE") || RN.equals("INTRUDER") || RN.equals("GUEST") || RN.equals("EMPLOYEE") || RN.equals("CEO") || RN.equals("CONFERENCE") || RN.equals("UNKNOWN") || RN.equals("MAP")){ //<>//
  Door door = new Door(RN, DTA, a, t); //<>//
  int d = day();    // Values from 1 - 31
  int m = month();  // Values from 1 - 12
  int y = year();   // 2003, 2004, 2005, etc.
  int h = hour();   // Values in military time
  int min = minute(); // Values from 00 - 59
  int sec = second(); //Vales from 00 - 59

  String month = "";
  switch(m) {
  case 1:
    month = "Jan";
    break;
  case 2:
    month = "Feb";
    break;
  case 3:
    month = "Mar";
    break;
  case 4:
    month = "Apr";
    break;
  case 5:
    month = "May";
    break;
  case 6:
    month = "Jun";
    break;
  case 7:
    month = "Jul";
    break;
  case 8:
    month = "Aug";
    break;
  case 9:
    month = "Sep";
    break;
  case 10:
    month = "Oct";
    break;
  case 11:
    month = "Nov";
    break;
  case 12:
    month = "Dec";
    break;
  }
  t = String.valueOf(d) + "-" + String.valueOf(month) + "-" +String.valueOf(y) + " " + String.valueOf(h) + ":" + String.valueOf(min) + ":" + String.valueOf(sec);
  door.setTime(t); //t is DATETIME




  if ((!(mes.equals("FIRE")))&&(!(mes.equals("INTRUDER"))&&(!(mes.equals("MAP"))))) { //this chunk of code should not run during special cases FIRE and INTRUDER
    int doornum = Integer.parseInt(DTA);
    boolean tmp = door.getAccess();
    switch(doornum) {
    case 0: //maindoor
      if (tmp) {
        maindoor = true;
      }
      b1 = millis();
      break;
    case 1: //confdoor
      if (tmp) {
        confdoor = true;
      }
      b2 = millis();
      break;
    case 2: //bathroomdoor1
      if (tmp) {
        bathroomdoor1 = true;
      }
      b3 = millis();
      break;
    case 3: //bathroomdoor2
      if (tmp) {
        bathroomdoor2 = true;
      }
      b4 = millis();
      break;
    case 4: //bigofficedoor
      if (tmp) {
        bigofficedoor = true;
      }
      b5 = millis();
      break;
    case 5: //officedoor2
      if (tmp) {
        officedoor1 = true;
      }
      b6 = millis();
      break;
    case 6: //officedoor1
      if (tmp) {
        officedoor2 = true;
      }
      b7 = millis();
      break;
    case 7: //kitchendoor
      if (tmp) {
        kitchendoor = true;
      }
      b8 = millis();
      break;
    }
  }
  
  if (mes.equals("MAP")) {
    //do nothing;
  }
  else {
  String dir = "./database.csv";
  String[] head = {"RoleName", "DoorToAccess", "AccessStatus", "DATETIME", "latlong"};
  File tempFile = new File(dir);
  Boolean check = tempFile.exists();
  String[] dis = {RN, DTA, String.valueOf(a), t, latlng};
  try(CSVWriter writer = new CSVWriter(new FileWriter(tempFile, true))) {
    if (check) {
      writer.writeNext(dis);
    } else {
      List<String[]> entries = new ArrayList<>();
      entries.add(head);
      entries.add(dis);
      writer.writeAll(entries);
    }
  }
  catch(Exception e) {
    print(e);
  }
  }
}
else{
  System.out.println("Your Role Name is invalid. Pleace get your NFC card checked out by IT");
}
}
