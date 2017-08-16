import java.util.*;
import java.util.stream.Collectors;


/**
 * Grab Snaffles and try to throw them through the opponent's goal!
 * Move towards a Snaffle and use your team id to determine where you need to throw it.
 **/
class Player {
    static Map<Integer, Wizard> wizards = new HashMap<>();
    static Map<Integer, Thing> opWizards = new HashMap<>();
    static Map<Integer, Snaffle> snaffles = new HashMap<>();
    static Map<Integer, Thing> bludgers = new HashMap<>();

    static List<Integer> updatedSnaffles = null;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        Global.myTeamId = in.nextInt(); // if 0 you need to score on the right of the map, if 1 you need to score on the left
        System.err.println("myteamId is :" + Global.myTeamId);

        Util.setCenterOfGoal();

        Global.turnCount = 0;
        Global.usedSpellCost = 0;

        // game loop
        while (true) {
            updatedSnaffles = new ArrayList<>();

            int myScore = in.nextInt();
            int myMagic = in.nextInt();
            int opponentScore = in.nextInt();
            int opponentMagic = in.nextInt();
            int entities = in.nextInt(); // number of entities still in game
            for (int i = 0; i < entities; i++) {
                int entityId = in.nextInt(); // entity identifier
                String entityType = in.next(); // "WIZARD", "OPPONENT_WIZARD" or "SNAFFLE" (or "BLUDGER" after first league)
                int x = in.nextInt(); // position
                int y = in.nextInt(); // position
                int vx = in.nextInt(); // velocity
                int vy = in.nextInt(); // velocity
                int state = in.nextInt(); // 1 if the wizard is holding a Snaffle, 0 otherwise
                Thing t = new Thing(x, y, vx, vy, state, entityId, entityType);
                //System.err.println(t.toString());
                if (Global.turnCount == 0) {
                    init(entityType, t);
                } else {
                    update(entityType, t);
                }
            }

            Util.removeNeedlessSnaffles(updatedSnaffles, snaffles);

            //showWizards();
//            System.err.println("show snaffles");
//            showSnaffles();

            List<String> res = new ArrayList<>();
            int prevTarget = -1;
            for (Map.Entry<Integer, Wizard> w : wizards.entrySet()) {
                res.add(w.getValue().generateCommand(wizards, opWizards, snaffles, bludgers, prevTarget));
                prevTarget = w.getValue().targetId;
            }

            for (int i = 0; i < 2; i++) {
                System.out.println(res.get(i));
            }
            Global.turnCount++;
        }
    }

    public static void init(String entityType, Thing t) {
        switch (entityType) {
            case "WIZARD":
                wizards.put(t.entityId, new Wizard(t));
                break;
            case "OPPONENT_WIZARD":
                opWizards.put(t.entityId, t);
                break;
            case "SNAFFLE":
                snaffles.put(t.entityId, new Snaffle(t));
                updatedSnaffles.add(t.entityId);
                break;
            case "BLUDGER":
                bludgers.put(t.entityId, t);
                break;
            default:
        }
    }

    public static void update(String entityType, Thing t) {
        switch (entityType) {
            case "WIZARD":
                Wizard w = wizards.get(t.entityId);
                w.update(t);
                break;
            case "OPPONENT_WIZARD": {
                Thing tmp = opWizards.get(t.entityId);
                tmp.update(t);
                opWizards.put(tmp.entityId, tmp);
                break;
            }
            case "SNAFFLE": {
                Snaffle tmp = snaffles.get(t.entityId);
                if (tmp != null) {

                    updatedSnaffles.add(tmp.entityId);

                    tmp.update(t);
                    snaffles.put(tmp.entityId, tmp);
                }
                break;
            }
            case "BLUDGER": {
                Thing tmp = bludgers.get(t.entityId);
                tmp.update(t);
                bludgers.put(tmp.entityId, tmp);
                break;
            }
            default:
        }
    }

    static void showWizards() {
        for (Map.Entry<Integer, Wizard> w : wizards.entrySet()) {
            System.err.println(w.toString());
        }
    }

    static void showSnaffles() {
        for (Map.Entry<Integer, Snaffle> s : snaffles.entrySet()) {
            System.err.println(s.toString());
        }
    }


}

class Wizard extends Thing {

    public static final int SHOOT_WIDTH = 50;

    String command;
    long commandX, commandY, power;
    int targetId;
    String message = "";
    //boolean grabbing


    int runPower = 150;
    int throwPower = 500;

    public Wizard() {
    }

    public Wizard(Thing t) {
        super(t.x, t.y, t.vx, t.vy, t.state, t.entityId, t.entityType);
    }

    String generateCommand(Map<Integer, Wizard> wizards, Map<Integer, Thing> opWizards, Map<Integer, Snaffle> snaffles, Map<Integer, Thing> bludgers, int without) {
        this.message = "";

        String res;

        if (this.state == 0) {//持ってない
            if (willPetrificus(snaffles, opWizards)) {
                Global.usedSpellCost += Global.cPetrificus;
                res = "PETRIFICUS " + this.targetId;
                Global.castHistory.put(this.targetId, Global.turnCount);
            } else if (willFlipend(snaffles, Util.margeThings(wizards, opWizards, snaffles, bludgers))) {
                Global.usedSpellCost += Global.cFlipend;
                res = "FLIPENDO " + this.targetId;
            } else if (willAccio(wizards, snaffles, opWizards)) {
                Global.usedSpellCost += Global.cAccio;
                res = "ACCIO " + this.targetId;
            } else {
                res = move(snaffles, opWizards, without);
            }

        } else {//持ってる
            res = throwSnaffle(opWizards, bludgers);

        }
        if (!this.message.equals(""))
            res += " " + this.message;
        //res += " hello";
        return res;
    }

    private String throwSnaffle(Map<Integer, Thing> opWizards, Map<Integer, Thing> bludgers) {
        //果たして下だけの場合に比べてthrowは良くなるのか！？

        if(Util.dist2EnemyGoal(this)<=8000 || Util.getMinDist2Enemies(this,opWizards)>=5000){
            if (Global.myTeamId == 0) {
                return "THROW 16000 3750 500";
            } else {
                return  "THROW 0 3750 500";
            }
        }


        String res;
        command = "THROW";
        power = throwPower;
        List<Thing> obstacles = Util.margeThings(null, opWizards, null, bludgers);


        double maxDist = Double.MIN_VALUE;
        long maxDistX;
        long maxDistY;

        if (Global.myTeamId == 0) {
            maxDistX = 16000;
            maxDistY = 3750;
//            res = "THROW 16000 3750 500";
        } else {
            maxDistX = 0;
            maxDistY = 3750;
//            res = "THROW 0 3750 500";
        }

        Thing t = this.getMoved();

        final long throwR = 1000;
        final double RANGE = 45;
        final double lower = -RANGE;
        final double higher = RANGE;
        for (double deg = lower; deg < higher; deg += (higher - lower) / 5) {
            double x = throwR * Math.cos(Math.toRadians(deg));

            if (Global.myTeamId == 1) x *= -1;

            double y = throwR * Math.sin(Math.toRadians(deg));

            long x2 = t.x + Util.round(x);//目標地点
            long y2 = t.y + Util.round(y);
            System.err.println("tar:"+x2+","+y2);

            if(!Util.isIn(x2,y2))
                continue;

            double minDist = Double.MAX_VALUE;
            long minDistX = 0;
            long minDistY = 0;
            for (Thing it : obstacles) {
                Thing it2 = it.getMoved();
                double dist = Util.distPoints(x2, y2, it2.x, it2.y);
                if (dist < minDist) {
                    minDistX = x2;
                    minDistY = y2;
                }
            }
            if (maxDist < minDist) {
                maxDistX = minDistX;
                maxDistY = minDistY;
            }
        }

        //補正
        maxDistX -= this.vx;
        maxDistY -= this.vy;

        this.commandX = maxDistX;
        this.commandY = maxDistY;

        return "THROW " + maxDistX + " " + maxDistY + " " + throwPower;
    }

    String move(Map<Integer, Snaffle> snaffles, Map<Integer, Thing> opWizards, int without) {
        Thing t = Util.getNearest(this, snaffles, without);

        if (t == null) {
            t = Util.getNearest(this, snaffles, -1);
        }

        if (t != null) {
            command = "MOVE";
            x = t.x;
            y = t.y;
            targetId = t.entityId;
            power = runPower;
            return "MOVE " + t.x + " " + t.y + " " + runPower;
        } else {
            return "MOVE 7500 3500 150";
        }
    }

    boolean willPetrificus(Map<Integer, Snaffle> snaffles, Map<Integer, Thing> opWizards) {
        if (!isCastable(Global.cPetrificus))
            return false;



        for (Map.Entry<Integer, Snaffle> e : snaffles.entrySet()) {
            //自陣側ゴールに近いsnaffle かつ 高速でゴールに向かっているものについて、
            if (Util.distMyGoal(e.getValue()) <= AIParams.PETRIF_FIELD &&
                    Util.getSpeed(e.getValue()) >= AIParams.PETRIF_SPEED &&
                    Util.isGoingToMyGoal(e.getValue())) {
                long myDist2Snaffle = Util.getDistance(this, e.getValue());
                //boolean petri = true;
                //全ての相手よりも自分のほうが近ければ
                /*for (Map.Entry<Integer, Thing> w : opWizards.entrySet()) {
                    if (myDist2Snaffle > Util.getDistance(w.getValue(), e.getValue())) {
                        //petri = false;
                        return false;
                    }
                }*/
                //if (petri) {
                Integer checker = Global.castHistory.get(e.getValue().entityId);
                checker = checker == null ? 0 : checker;
                if ((checker + AIParams.reCastPET) < Global.turnCount) {
                    this.targetId = e.getValue().entityId;
                    return true;
                }
                //}
            }
        }
        return false;
    }

    boolean willAccio(Map<Integer, Wizard> wizards, Map<Integer, Snaffle> snaffles, Map<Integer, Thing> opWizards) {
        if (!isCastable(Global.cAccio))
            return false;

        for (Map.Entry<Integer, Snaffle> e : snaffles.entrySet()) {
            //snaffleが自陣側ゴールに近くて、引き寄せられる範囲なら
            boolean f1=Util.distMyGoal(e.getValue()) <= AIParams.ACCIO_FIELD;
            System.err.println("ac_near:"+f1);
            boolean f2=Util.getDistance(this, e.getValue()) <= AIParams.ACCIO_DIST;
            System.err.println("ac_dist:"+f2);
            if ( f1&& f2) {

                //近くに相手がいれば
                for (Map.Entry<Integer, Thing> opW : opWizards.entrySet()) {
                    if (Util.getDistance(e.getValue(), opW.getValue()) <= AIParams.ACCIO_SURROUNDIST) {
                        boolean enemyNear=enemyNear(wizards, opWizards, e.getValue());
                        System.err.println("enear:"+enemyNear);
                        if (enemyNear) {
                            this.targetId = e.getValue().entityId;
                            return true;
                        }
                    }
                }

            }
        }
        return false;
    }

    boolean enemyNear(Map<Integer, Wizard> wizards, Map<Integer, Thing> opWizards, Snaffle s) {
        long eMinDist = Long.MAX_VALUE;
        for (Map.Entry<Integer, Thing> opw : opWizards.entrySet()) {
            long d = Util.getDistance(opw.getValue(), s);
            if (eMinDist > d) {
                eMinDist = d;
            }
        }
        long mMinDist = Long.MAX_VALUE;
        for (Map.Entry<Integer, Wizard> w : wizards.entrySet()) {
            long d = Util.getDistance(w.getValue(), s);
            if (mMinDist > d) {
                mMinDist = d;
            }
        }
        return eMinDist < mMinDist;
    }

    boolean willFlipend(Map<Integer, Snaffle> snaffles, List<Thing> obstacles) {
        if (!isCastable(Global.cFlipend))   //|| dist2Goal() < AIParams.FLIP_FIELD
            return false;

        // this.message+="castable ";
        //ゴール側のモノを列挙
        int goalX = Global.targetGoalX;
        int mid = (Global.pollUpper + Global.pollLower) / 2;
        List<Thing> objects = new ArrayList<>();
        for (Thing t : obstacles) {
            if (isOffenceSide(goalX, mid, t) && t.entityId != this.entityId) {
                System.err.println("gt:" + t.entityId);
                objects.add(t);
            }
        }

        //goal側のsnaffleを対象にする
        Map<Integer, Snaffle> targets = goalSideSnaffles(snaffles);

        //１つも無いなら入るかどうかだけ確かめる
        if (objects.isEmpty()) {
            // this.message += "no obstacle";
            for (Map.Entry<Integer, Snaffle> e : targets.entrySet()) {
                if (isShootable(e.getValue())) {
                    this.targetId = e.getValue().entityId;
                    //System.err.println("flip because no obstacle");
                    return true;
                }
            }
            return false;
        }

        boolean shoot = false;
        double maxDist = Double.MIN_VALUE;
        int targetId = -1;
        //this.message += "obst ";
        //ゴール側の全ての対象を調べる
        for (Map.Entry<Integer, Snaffle> e : targets.entrySet()) {
            //現在地からゴールに向かって打てる
            if (isShootable(e.getValue())) {
                //this.message+="shootable ";
                Thing right;
                Thing left;
                if (this.x < e.getValue().x) {
                    right = this;
                    left = e.getValue();
                } else {
                    right = e.getValue();
                    left = this;
                }

                Thing from = Global.myTeamId == 0 ? right : left;

                long gy = Util.getExtendedPoint(left.x, left.y, right.x, right.y);

                long goalx = Global.targetGoalX;
                goalx += Global.myTeamId == 0 ? +50 : -50;
                //最も障害物との距離が遠い
                for (Thing t : objects) {
                    Thing t2 = t.getMoved();
                    double d = Util.getDistObj2line(from.x, from.y, goalx, gy, t2.x, t2.y);//goalx..補正がかかった方
                    if (maxDist < d && (t2.r + AIParams.FLIP_BUFF) < d) {
                        shoot = true;
                        maxDist = d;
                        targetId = e.getValue().entityId;
                    }
                }
            }
        }
        if (shoot) {
            //this.message += "shble";
            this.targetId = targetId;
        } else {
            //this.message += "cant";
        }
        return shoot;

    }

    long dist2Goal() {
        return Math.abs(Global.targetGoalX - this.x);
    }

    Map<Integer, Snaffle> goalSideSnaffles(Map<Integer, Snaffle> snaffles) {
        Map<Integer, Snaffle> res = new HashMap<>();
        for (Map.Entry<Integer, Snaffle> e : snaffles.entrySet()) {
            if (Global.myTeamId == 0) {
                if (this.x <= e.getValue().x)
                    res.put(e.getKey(), e.getValue());
            } else {
                if (e.getValue().x <= this.x)
                    res.put(e.getKey(), e.getValue());
            }

        }
        return res;
    }

    /* //コストと状況からうつべきかどうか判断

    boolean willFlipend(List<Thing> list) {
        if (!isCastable(20))
            return false;

        //ゴール側のモノのみ調べる
        int goalX = Global.myTeamId == 0 ? 16000 : 0;
        int mid = (Global.pollUpper + Global.pollLower) / 2;
        List<Thing> objects = new ArrayList<>();
        for (Thing t : list) {
            if (isOffenceSide(goalX, mid, t))
                objects.add(t);
        }

        //ゴール側のものが１つも無いなら打ち込む場所は真ん中
        if (objects.isEmpty()) {
            this.commandX = goalX;
            this.commandY = mid;
            return true;    //falseのほうがいいかなあ
        }

        //ゴール範囲をglobal.divnumで割った分だけ調べる
        List<Integer> goalY = new ArrayList<>();
        for (int i = Global.pollLower; i <= Global.pollUpper; i += Global.pollDiff) {
            if (i == Global.pollLower || i == Global.pollUpper)
                continue;
            goalY.add(i);
        }

        boolean flipend = false;
        //当たらないコースがあれば flipend=true
        //また、他の物体からもっとも離れるようなコースを選択して、Wizardにメモる
        double maxDist = Double.MIN_VALUE;
        for (Integer tmpY : goalY) {
            for (Thing t : objects) {
                double d = Util.getDistObj2line(this.x, this.y, goalX, tmpY, t.x, t.y);
                if (d > (t.r+Global.SNAF_R+SHOOT_WIDTH) && d > maxDist) { //打つのに十分な幅があり、今までの
                    flipend=true;
                    this.commandX = t.x;
                    this.commandY = t.y;

                }
            }
        }
        return flipend;
    }*/

    //自分よりゴール側にあるsniffleを打ってみる
    boolean isShootable(Thing t) {
        Thing me = this.getMoved();
        Thing moved = t.getMoved();
        {
            long dist = Util.getDistance(me, moved);
            //System.err.println(dist);
            if (dist > AIParams.SHOOTRANGE) {
                this.message += " too long";
                return false;
            }
        }
        {
            long dist = Util.dist2EnemyGoal(t);
            if (dist > 10000)
                return false;
        }


        //if(moved.state)


        /*double angG = Util.getAngleOfVectors(Global.targetGoalX - this.x, Global.pollUpper - this.y, Global.targetGoalX - this.x, Global.pollLower - this.y);
        double angT = Util.getAngleOfVectors(Global.targetGoalX - this.x, Global.pollUpper - this.y, t.x - this.x, t.y - this.y);
        System.err.println(angG +" " +angT);
        double diff = angT - angG;
        this.message += " g: " + angG + " t:" + angT + " ";
        return angG > angT;*/
        if (Global.myTeamId == 0) {
            long gy = Util.getExtendedPoint(me.x, me.y, moved.x, moved.y);
            System.err.println("fId,tId,gy:" + me.entityId + "," + moved.entityId + "," + gy);
            long goalx = Global.targetGoalX;
            goalx += Global.myTeamId == 0 ? +50 : -50;
            return Util.isIntersect(me.x, me.y, goalx, gy, Global.targetGoalX, Global.pollUpper, Global.targetGoalX, Global.pollLower);
        } else {
            long gy = Util.getExtendedPoint(moved.x, moved.y, me.x, me.y);
            System.err.println("fId,tId,gy:" + me.entityId + "," + moved.entityId + "," + gy);
            long goalx = Global.targetGoalX;
            goalx += Global.myTeamId == 0 ? +50 : -50;
            return Util.isIntersect(me.x, me.y, goalx, gy, Global.targetGoalX, Global.pollLower, Global.targetGoalX, Global.pollUpper);
        }
    }

    boolean isOffenceSide(long targetX, long targetY, Thing t) {
        boolean flag;
        if (Global.myTeamId == 0) {
            flag = this.x <= t.x;
        } else {
            flag = t.x <= this.x;
        }
        return flag && Util.rad2deg(Util.getAngleOfVectors(targetX - this.x, targetY - this.y, t.x - this.x, t.y - this.y)) <= 120.0;
    }

    boolean isCastable(int cost) {
        return Global.turnCount - Global.usedSpellCost >= cost;
    }

}

class Snaffle extends Thing {
    public Snaffle() {
    }

    public Snaffle(Thing t) {
        super(t.x, t.y, t.vx, t.vy, t.state, t.entityId, t.entityType);
    }

    @Override
    public String toString() {
        return "Snaffle" + super.toString();
    }
}

class Util {
    //-------------幾何関係

    //AB to CD が交差してるか返す
    //http://qiita.com/ykob/items/ab7f30c43a0ed52d16f2
    static boolean isIntersect(long ax, long ay, long bx, long by, long cx, long cy, long dx, long dy) {
        long ta = (cx - dx) * (ay - cy) + (cy - dy) * (cx - ax);
        long tb = (cx - dx) * (by - cy) + (cy - dy) * (cx - bx);
        long tc = (ax - bx) * (cy - ay) + (ay - by) * (ax - cx);
        long td = (ax - bx) * (dy - ay) + (ay - by) * (ax - dx);

        boolean flag = tc * td < 0 && ta * tb < 0;
        if (flag) {
            System.err.println(ta + "," + tb + "," + tc + "," + td);
            System.err.println("(" + ax + "," + ay + "," + bx + "," + by + ")(" + cx + "," + cy + "," + dx + "," + dy + ") insersects.");
        }

        return flag;
    }

    //round half away from zero: ゼロから遠い方へ丸める
    //http://www.ftext.org/text/subsubsection/2365
    static long getExtendedPoint(long ax, long ay, long bx, long by) {
        long goalx = Global.targetGoalX;
        goalx += Global.myTeamId == 0 ? +50 : -50;
        double d = (double) (by - ay) / (double) (bx - ax) * (goalx - ax) + ay;
        return (long) Math.ceil(Math.abs(d)) * (d > 0 ? 1 : -1);
    }

    static double getAngleOfVectors(long v1x, long v1y, long v2x, long v2y) {
        double l1 = getVectorLength(v1x, v1y);
        double l2 = getVectorLength(v2x, v2y);
        double cosTheta = (double) prodVector(v1x, v1y, v2x, v2y) / (l1 * l2);
        return Math.acos(cosTheta); //radian
    }

    static double rad2deg(double radian) {
        return radian * 180.0 / Math.PI;
    }

    static double getVectorLength(long x, long y) {
        return Math.sqrt(x * x + y * y);
    }

    //from to のベクトルとtargetの点との距離を測る
    static double getDistObj2line(long fromX, long fromY, long toX, long toY, long targetX, long targetY) {
        long v1x = toX - fromX;
        long v1y = toY - fromY;
        long v2x = targetX - fromX;
        long v2y = targetY - fromY;

        long d = Math.abs(crossVector(v1x, v1y, v2x, v2y));
        double l = distPoints(fromX, fromY, toX, toY);
        return (double) d / l;
    }

    //ベクトルの内積
    static long prodVector(long v1x, long v1y, long v2x, long v2y) {
        return v1x * v2x + v1y * v2y;
    }

    //ベクトルの外積
    static long crossVector(long v1x, long v1y, long v2x, long v2y) {
        return v1x * v2y - v1y * v2x;
    }

    static double distPoints(long x1, long y1, long x2, long y2) {
        long diffx = x2 - x1;
        long diffy = y2 - y1;
        return Math.sqrt(diffx * diffx + diffy * diffy);
    }

    static long getDistance(Thing from, Thing to) {
        return (long) Math.sqrt((to.x - from.x) * (to.x - from.x) + (to.y - from.y) * (to.y - from.y));
    }

    static double getSpeed(Thing t) {
        return Math.sqrt(t.vx * t.vx + t.vy * t.vy);
    }

    // -1 if there is no without
    static Thing getNearest(Thing from, Map<Integer, Snaffle> map, int without) {
        long minDist = Long.MAX_VALUE;
        Thing minThing = null;
        for (Map.Entry<Integer, Snaffle> e : map.entrySet()) {
//            if(e.getValue().isOut())
//                continue;

            long dist = getDistance(from, e.getValue());
            if (from.entityId != e.getValue().entityId && e.getValue().entityId != without && dist < minDist) {
                minDist = dist;
                minThing = e.getValue();
            }
        }
        return minThing;
    }

    static Thing getNearest(Thing from, Map<Integer, Snaffle> list) {
        return getNearest(from, list, -1);
    }

    //-------------その他
    static void removeNeedlessSnaffles(List<Integer> updated, Map<Integer, Snaffle> snaffles) {
        List<Integer> list = snaffles.entrySet().stream().filter(e -> !updated.contains(e.getKey())).map(Map.Entry::getKey).collect(Collectors.toList());
        list.forEach(snaffles::remove);
    }

    static List<Thing> margeThings(Map<Integer, Wizard> wizards, Map<Integer, Thing> opWizards, Map<Integer, Snaffle> snaffles, Map<Integer, Thing> bludgers) {
        List<Thing> res = new ArrayList<>();
        /*if(wizards!=null){
            for (Map.Entry<Integer,Wizard> e:wizards.entrySet()){
                res.add(e.getValue());
            }
        }*/
        if (opWizards != null) {
            for (Map.Entry<Integer, Thing> e : opWizards.entrySet()) {
                res.add(e.getValue());
            }
        }

        if (snaffles != null) {
            for (Map.Entry<Integer, Snaffle> e : snaffles.entrySet()) {
                res.add(e.getValue());
            }
        }
        if (bludgers != null) {
            for (Map.Entry<Integer, Thing> e : bludgers.entrySet()) {
                res.add(e.getValue());
            }
        }
        return res;
    }

    static void setCenterOfGoal() {
        Global.myGoalX = Global.myTeamId == 0 ? 0 : 16000;
        Global.myGoalY = 3750;
        Global.targetGoalX = Global.myTeamId == 0 ? 16000 : 0;
        Global.targetGoalY = 3750;
    }

    static double getFriction(String entityType) {
        switch (entityType) {
            case "WIZARD":
                return Global.WIZ_FRICTION;
            case "OPPONENT_WIZARD":
                return Global.WIZ_FRICTION;
            case "SNAFFLE":
                return Global.SNAF_FRICTION;
            case "BLUDGER":
                return Global.BLUD_FRICTION;
            default:
                return 1.0;
        }
    }

    static long round(double num) {
        return (long) (Math.ceil(Math.abs(num)) * (num > 0 ? 1 : -1));
    }

    static long distMyGoal(Thing t) {
        return Math.abs(Global.myGoalX - t.x);
    }

    static long dist2EnemyGoal(Thing t) {
        return Math.abs(Global.targetGoalX - t.x);
    }

    static boolean isGoingToMyGoal(Thing t) {
        return Global.myTeamId == 0 ? t.vx < 0 : t.vx > 0;
    }

    static boolean isGoingToEnemyGoal(Thing t) {
        return Global.myTeamId == 0 ? t.vx > 0 : t.vx < 0;
    }

    public static boolean isOut(long x, long y) {
        return !(0 < x && x < 16000 && 0 < y && y < 7500);
    }

    public static boolean isIn(long x, long y) {
        return (0 < x && x < 16000 && 0 < y && y < 7500);
    }
    public static long getMinDist2Enemies(Thing t,Map<Integer,Thing> opWizards){
        long minDist=Long.MAX_VALUE;
        for(Map.Entry<Integer,Thing> e:opWizards.entrySet()){
            long d=getDistance(t,e.getValue());
            if(minDist>d){
                minDist=d;
            }
        }
        return minDist;
    }

    /*public static boolean inEmargency(Snaffle s){
        return Util.distMyGoal(s)
    }*/
}


class Thing {
    public Thing() {
    }

    public Thing(int x, int y, int vx, int vy, int state, int entityId, String entityType) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.state = state;
        this.entityId = entityId;
        this.entityType = entityType;
        switch (this.entityType) {
            case "WIZARD":
                this.r = 400;
                break;
            case "OPPONENT_WIZARD":
                this.r = 400;
                break;
            case "SNAFFLE":
                this.r = 150;
                break;
            case "BLUDGER":
                this.r = 200;
                break;
            default:
        }
    }

    public Thing getMoved() {
        Thing t = new Thing();
        t.x = this.x + this.vx;
        t.y = this.y + this.vy;
        t.vx = (int)Util.round(this.vx * Util.getFriction(this.entityType));
        t.vy = (int)Util.round(this.vy * Util.getFriction(this.entityType));
        t.state = this.state; //ホントは違う
        t.entityId = this.entityId;
        t.entityType = this.entityType;
        t.r = this.r;
        //System.err.println("moved x,y,id:"+t.x+" "+t.y+" "+t.entityId);
        return t;
    }

    public void update(Thing t) {
        this.x = t.x;
        this.y = t.y;
        this.vx = t.vx;
        this.vy = t.vy;
        this.state = t.state;
        this.entityId = t.entityId;
    }

    @Override
    public String toString() {
        return "Thing{" +
                "x=" + x +
                ", y=" + y +
                ", vx=" + vx +
                ", vy=" + vy +
                ", state=" + state +
                ", entityId=" + entityId +
                ", entityType='" + entityType + '\'' +
                '}';
    }

    int x;
    int y;
    int vx;
    int vy;
    int state;
    int entityId;
    String entityType;
    int r;
}

class Global {
    static int turnCount = 0;
    static int usedSpellCost = 0;
    static int myTeamId = 0;
    static int divideGoalNum = 100;

    static double WIZ_FRICTION = 0.75;
    static double SNAF_FRICTION = 0.75;
    static double BLUD_FRICTION = 0.9;

    static int targetGoalX;
    static int targetGoalY;

    static int myGoalX;
    static int myGoalY;

    static int cFlipend = 20;
    static int cAccio = 20;
    static int cPetrificus = 10;
    static int obliviate = 5;

    static int pollLower = 2050;
    static int pollUpper = 5450;
    static int pollDiff = (pollUpper - pollLower) / divideGoalNum;

    static int buffer = 300;
    static int shootLower = pollLower + buffer;
    static int shootUpper = pollUpper - buffer;

    static int SNAF_R = 150;
    static int WIZ_R = 400;
    static int OPWIZ_R = 400;
    static int BLUD_R = 200;

    static Map<Integer, Integer> castHistory = new HashMap<>();
}

class AIParams {
    static int SHOOTRANGE = 1500;

    static int PETRIF_SPEED = 1100;
    static int ACCIO_DIST = 4000;

    static int ACCIO_FIELD = 4500;
    static int FLIP_FIELD = 5000;
    static int PETRIF_FIELD = 10000;

    static int ACCIO_SURROUNDIST = 3000;
    static int FLIP_BUFF = 500;

    static int reCastPET = 3;
}