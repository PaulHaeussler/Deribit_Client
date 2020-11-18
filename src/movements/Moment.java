package movements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Moment {

    public static ArrayList<String> users = new ArrayList<>();

    public static Moment firstMoment;

    public static double FEE_RATE = 0.2;

    public Movement movement;
    public Moment prevMoment;

    public double change;
    public double timestamp;

    public double totalBalanceOld;
    public double totalBalanceNew = 0.0;
    public HashMap<String, Double> userCapitalOld = new HashMap<>();
    public HashMap<String, Double> userCapitalNew = new HashMap<>();
    public HashMap<String, Double> capitalShare = new HashMap<>();
    public HashMap<String, Double> usersShare = new HashMap<>();
    public HashMap<String, Double> feePaid = new HashMap<>();
    public HashMap<String, Double> effectiveProfit = new HashMap<>();

    public Moment(Movement mov, Moment prev) throws Exception {
        movement = mov;
        prevMoment = prev;

        change = mov.getChange();
        timestamp = mov.getTimestamp();


        if(prevMoment == null) {
            firstMoment = this;
            for(String user : users) {
                userCapitalOld.put(user, 0.0);
                totalBalanceOld = 0.0;
            }
        } else {
            if(timestamp < prev.timestamp) throw new Exception("Previous Moment is younger than this!");
            for(String user : users) {
                userCapitalOld.put(user, prevMoment.userCapitalNew.get(user));
            }
            totalBalanceOld = prev.totalBalanceNew;
        }

        HashMap<String, Double> temp = new HashMap<>();
        for(Map.Entry<String, Double> entry : userCapitalOld.entrySet()){
            temp.put(entry.getKey(), entry.getValue());
        }


        if(movement instanceof Deposit || movement instanceof  Withdrawal){
            temp.put(movement.getUser(), temp.get(movement.getUser()) + change);
        } else {
            for(String user : users){

                if(userCapitalOld.get(user) > 0.0) {
                    capitalShare.put(user, (userCapitalOld.get(user) / totalBalanceOld));
                    usersShare.put(user, change * capitalShare.get(user));
                } else {
                    capitalShare.put(user, 0.0);
                    usersShare.put(user, 0.0);
                }

                if(!user.equals("Paul")){
                    double fee = usersShare.get(user) * FEE_RATE;
                    feePaid.put(user, fee);
                    Double paulFee = feePaid.get("Paul");
                    if(paulFee == null) {
                        feePaid.put("Paul", fee * -1);
                    } else {
                        feePaid.put("Paul", feePaid.get("Paul") + fee * -1);
                    }
                    effectiveProfit.put(user, usersShare.get(user) - feePaid.get(user));
                    temp.put(user, userCapitalOld.get(user) + effectiveProfit.get(user));
                } else {
                    if(feePaid.get("Paul") == null) feePaid.put("Paul", 0.0);
                }
            }
            effectiveProfit.put("Paul", usersShare.get("Paul") - feePaid.get("Paul"));
            temp.put("Paul", userCapitalOld.get("Paul") + effectiveProfit.get("Paul"));
        }

        for(Map.Entry<String, Double> entry : temp.entrySet()){
            userCapitalNew.put(entry.getKey(), entry.getValue());
            totalBalanceNew += entry.getValue();
        }
    }
}
