import java.util.*;
import java.util.function.Consumer;


/**
 * Grab Snaffles and try to throw them through the opponent's goal!
 * Move towards a Snaffle and use your team id to determine where you need to throw it.
 **/
class Player {
    static long turnStartTime;

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);

        Global.myTeamId = in.nextInt(); // if 0 you need to score on the right of the map, if 1 you need to score on the left
        System.err.println("myteamId is :" + Global.myTeamId);

        Util.setCenterOfGoal();

        System.err.println("");

        Global.turnCount = 0;
        Global.usedSpellCost = 0;

        // game loop
        while (true) {
            turnStartTime = System.currentTimeMillis();

            int myScore = in.nextInt();
            int myMagic = in.nextInt();
            int opScore = in.nextInt();
            int opMagic = in.nextInt();
            int entities = in.nextInt(); // number of entities still in game

            ArrayList<Wizard> wizards = new ArrayList<>();
            ArrayList<Thing> opWizards = new ArrayList<>();
            ArrayList<Snaffle> snaffles = new ArrayList<>();
            ArrayList<Thing> bludgers = new ArrayList<>();

            for (int i = 0; i < entities; i++) {
                int entityId = in.nextInt();    // entity identifier
                String entityType = in.next();  // "WIZARD", "OPPONENT_WIZARD" or "SNAFFLE" (or "BLUDGER" after first league)
                int x = in.nextInt();   // position
                int y = in.nextInt();   // position
                int vx = in.nextInt();  // velocity
                int vy = in.nextInt();  // velocity
                int state = in.nextInt();   // 1 if the wizard is holding a Snaffle, 0 otherwise
                Thing t = new Thing(x, y, vx, vy, state, entityId, entityType);
                switch (entityType) {
                    case "WIZARD":
                        wizards.add(new Wizard(t));
                        break;
                    case "OPPONENT_WIZARD":
                        opWizards.add(new Wizard(t));
                        break;
                    case "SNAFFLE":
                        snaffles.add(new Snaffle(t));
                        break;
                    case "BLUDGER":
                        bludgers.add(t);
                        break;
                    default:
                }
            }

            List<String> res = solve(turnStartTime, new State(wizards, opWizards, snaffles, bludgers, myScore, myMagic, opScore, opMagic, Global.turnCount, 0));

            for (int i = 0; i < 2; i++) {
                for(String s:CONST.SPELL_STR)
                    if(res.get(i).contains(s))
                        Global.lastCastTurn=Global.turnCount;
                //System.err.println(res.get(i));
                System.out.println(res.get(i));
            }
            Global.turnCount++;
        }
    }

    static void inspectStartState(State s){
        System.err.println("snaffle size:" + s.snaffles.size());
    }

    static List<String> solve(long startTime, State startState) {
        long currentTime = System.currentTimeMillis();
        startState.setScore();
        State bestState = startState.clone();

        TreeSet<State> beam1 = new TreeSet<>();
        TreeSet<State> beam2 = new TreeSet<>();
        beam1.add(startState.clone());
        //System.err.println("beam1:" + beam1.first().toString());
        int turnsSimulated = 0;
        while (currentTime < (startTime + AIParams.SEARCH_DURATION)) {  //何を答えとするか？ => 読める中で一番良い盤面になる状態に繊維する

            State now = beam1.pollFirst();

            if (now == null) break;

            bestState = updateQueue(now, bestState, beam2, currentTime, startTime + AIParams.SEARCH_DURATION);
            //System.err.println("beam2Count:"+beam2.size());
            if (beam1.isEmpty()) {
                beam1 = beam2;
                beam2 = new TreeSet<>();
            }
            turnsSimulated++;
            currentTime = System.currentTimeMillis();
        }

        /*if(!beam2.isEmpty()) {
            showScores(beam2, 30);
        }else
            showScores(beam1,30);*/
        //assert !bestState.equals(startState);
        System.err.println("simulated turn: " + turnsSimulated);
        System.err.println("bestState: " + bestState.score + " " + bestState.firstCommand);
        bestState.descScore();

        if (bestState.firstCommand.get(0).equals("") || bestState.firstCommand.get(1).equals("")) {
            bestState.firstCommand = CONST.dummyCommand;
            System.err.println("command invalid");
        }
        return bestState.firstCommand;

    }

    /*static void showScores(TreeSet<State> set, int limit) {
        int counter = 0;
        for (State s : set) {

            if (counter == limit) break;

            System.err.println("score" + counter + ": " + s.score + "\n command: " + s.firstCommand.toString());
            counter++;
        }
    }*/

    static State updateQueue(State now, State bestState, TreeSet<State> queue, long currentTime, long limitTime) {
        // assert now != null;
        List<State> newStates = new ArrayList<>(); //まとめてevaluateするため一旦リストへ格納
        Snaffle closestSnaf0 = (Snaffle) Util.getClosestThing(now.wizards.get(0), now.snaffles);
        /*double closestSnaf0Dist = Util.getDistance(now.wizards.get(0),closestSnaf0);

        Snaffle closestSnaf1 = (Snaffle) Util.getClosestThing(now.wizards.get(1), now.snaffles);
        double closestSnaf1Dist = Util.getDistance(now.wizards.get(1),closestSnaf1);

        Thing closestOpWiz0 = Util.getClosestThing(now.wizards.get(0), now.opWizards);
        double closestOpWiz0Dist = Util.getDistance(now.wizards.get(0), closestOpWiz0);

        Thing closestOpWiz1 = Util.getClosestThing(now.wizards.get(1),now.opWizards);
        double closestOpWiz1Dist = Util.getDistance(now.wizards.get(1), closestOpWiz1);
*/
        //全方向移動・投球　または　魔法
        for (int dir0 = 0; dir0 <= CONST.RADIANS.length; dir0++) { //i==lenで使えるなら魔法を使う。snaffleを持っているなら必ず投げる
            for (int dir1 = 0; dir1 <= CONST.RADIANS.length; dir1++) {
                State tmp = now.clone();

                if (dir0 == CONST.RADIANS.length && dir1 == CONST.RADIANS.length) continue; //単純化のためdouble castはなし
                //System.err.println("dir1: " + dir1);
                //System.err.println("dir2: " + dir2);

                //移動または投げるdirのとき、wiz0,1を更新。指定できない目標座標ならbreak
                if (dir0 != CONST.RADIANS.length && updateStateForMoveAndThrow(dir0, 0, tmp)) {
                    //System.err.println("break at dir1=" + dir1);
                    break;
                }
                if (dir1 != CONST.RADIANS.length && updateStateForMoveAndThrow(dir1, 1, tmp)) {
                    //System.err.println("break at dir2=" + dir2);
                    continue;
                }

                //このブロックでnewStatesに追加
                if (dir0 == CONST.RADIANS.length) { //castする場合、どうcastするか決めてStateListに追加する
                    //assert !tmp.firstCommand.get(1).equals("");
                    //FLIPENDO
                    updateStateForSpellAndAddStateList(newStates, tmp, "FLIPENDO",
                            CONST.cFlipendo, CONST.FLIPENDO_POWER, tmp.myMagic, 0);

                    //Accio
                    updateStateForSpellAndAddStateList(newStates, tmp, "ACCIO",
                            CONST.cAccio, CONST.ACCIO_POWER, tmp.myMagic, 0);
                    updateStateForPetAndAddStateList(newStates, tmp, tmp.myMagic, 0);
                } else if (dir1 == CONST.RADIANS.length) {
                    //assert !tmp.firstCommand.get(0).equals("");
                    //FLIPENDO
                    updateStateForSpellAndAddStateList(newStates, tmp, "FLIPENDO",
                            CONST.cFlipendo, CONST.FLIPENDO_POWER, tmp.myMagic, 1);

                    //Accio
                    updateStateForSpellAndAddStateList(newStates, tmp, "ACCIO",
                            CONST.cAccio, CONST.ACCIO_POWER, tmp.myMagic, 1);
                    updateStateForPetAndAddStateList(newStates, tmp, tmp.myMagic, 1);
                } else {//移動する場合、
                    //assert !tmp.firstCommand.get(0).equals("") && !tmp.firstCommand.get(1).equals("");
                    newStates.add(tmp);
                }
            }

            currentTime = System.currentTimeMillis();
            if (currentTime > limitTime)
                break;
        }
        //System.err.println("size of new state:" + newStates.size());
        for (State s : newStates) {
            simulateTurnAndEvaluate(s);
            bestState = updateStates(s, bestState, queue);
        }

        //System.err.println("fScore:"+queue.first().score);
        //System.err.println("lScore:"+queue.last().score);
        return bestState;
    }

    //return state having higher score
    private static State getBestState(State newState, State bestState) {
        //assert isValidCommand(newState.firstCommand);

        return isValidCommand(bestState.firstCommand) ? (newState.score > bestState.score ? newState : bestState)
                : newState;
    }

    private static boolean isValidCommand(List<String> command) {
        return !(command.get(0).equals("") || command.get(1).equals(""));
    }

    //update treeset and return state having best score
    private static State updateStates(State newState, State bestState, TreeSet<State> nextStates) {
        nextStates.add(newState);

        if (nextStates.size() > AIParams.BEAM_WIDTH)
            nextStates.pollLast();

        return getBestState(newState, bestState);
    }


    // 移動するパターンの場合(dir != CONST.RADIANS.len)、目的座標を計算して次の速度を設定する
    // return: true if cannot use destination
    private static boolean updateStateForMoveAndThrow(int direction, int wizNum, State tmp/*,
                                                      Thing closestSnaf, Thing closestOpWiz*/) {
        //if(direction == CONST.RADIANS.length) return false;

        Wizard w = tmp.wizards.get(wizNum);
        int destX = (int) w.x + Util.getMoveTargetDiffX(CONST.RADIANS[direction]);
        int destY = (int) w.y + Util.getMoveTargetDiffY(CONST.RADIANS[direction]);

        if (!Util.inField(destX, destY)) {
            //System.err.println("break dest x:" + destX + " y:" + destY);
            return true;
        }

        if (tmp.wizards.get(wizNum).state == 0) { //snaffleを持っていなければ移動
            tmp.wizards.get(wizNum).vx += CONST.WIZ_THRUST / CONST.WIZ_M * Math.cos(CONST.RADIANS[direction]);
            tmp.wizards.get(wizNum).vy += CONST.WIZ_THRUST / CONST.WIZ_M * Math.sin(CONST.RADIANS[direction]);

            if (tmp.firstCommand.get(wizNum).equals(""))
                tmp.firstCommand.set(wizNum, "MOVE " + destX + " " + destY + " " + CONST.WIZ_THRUST);

        } else {
            Snaffle throwTarget = (Snaffle) Util.getClosestThing(tmp.wizards.get(0), tmp.snaffles);

            if (throwTarget == null) return false;

            throwTarget.vx += CONST.THROW_POWER / CONST.SNAF_M * Math.cos(CONST.RADIANS[direction]);
            throwTarget.vy += CONST.THROW_POWER / CONST.SNAF_M * Math.sin(CONST.RADIANS[direction]);
            if (tmp.firstCommand.get(wizNum).equals(""))
                tmp.firstCommand.set(wizNum, "THROW " + destX + " " + destY + " " + CONST.THROW_POWER);
        }
        return false;
    }

    /*private static boolean checkThrowDir(Wizard w, Thing closestOpwiz, int dir){
        if(Util.getDistance(w,closestOpwiz) > )
    }*/

    //spellName: FLIPENDO or ACCIO, wizardId: 0 or 1
    private static void updateStateForSpellAndAddStateList(List<State> newStates, State baseState, String spellName, int spellCost, double spellPower, int myMagic, int wizNum) {
        if (spellCost < myMagic) {
            for (int i = 0; i < baseState.snaffles.size(); i++) { //Flipendoを打つのはsnaffleに対してのみ
                State tmp = baseState.clone();
                Snaffle tmpTargetSnaf = baseState.snaffles.get(i);
                //accioなら180度回転
                double angle = spellName.equals("FLIPENDO") ? Util.getRadianAngle(tmp.wizards.get(wizNum), tmpTargetSnaf)
                        : Util.getRadianAngle(tmpTargetSnaf, tmp.wizards.get(wizNum));
                double deg = Math.toDegrees(angle);

                //効果ある方向に打つか
                if (-90 <= deg && deg <= 90) {
                    if (Global.myTeamId == 1)
                        continue;
                } else {
                    if (Global.myTeamId == 0)
                        continue;
                }

                double dist = Util.getDistance(tmp.wizards.get(wizNum), tmpTargetSnaf);
                double acc = Util.getSpellAcc(dist, spellPower);

                //ACCIOなら遠すぎないかチェック
                if(spellName.equals("ACCIO") && dist > AIParams.ACCIO_DIST_MAX)
                    continue;

                //FLIPENDOなら10ターン後に入るかチェック
                if(spellName.equals("FLIPENDO") &&
                        (dist > AIParams.FLIP_DIST_MAX || !willPassGoal(tmpTargetSnaf, acc, angle)))
                    continue;

                tmp.snaffles.get(i).vx += acc / CONST.SNAF_M * Math.cos(angle);
                tmp.snaffles.get(i).vy += acc / CONST.SNAF_M * Math.sin(angle);

                tmp.myMagic -= spellCost;
                if (tmp.firstCommand.get(wizNum).equals(""))
                    tmp.firstCommand.set(wizNum, spellName + " " + tmp.snaffles.get(i).entityId);
                //assert !tmp.firstCommand.get(0).equals("") && !tmp.firstCommand.get(1).equals("");
                newStates.add(tmp);
            }
        }
    }

    private static boolean willPassGoal(Snaffle snaf, double acc, double ang){
        Snaffle tmp = snaf.clone();
        tmp.vx += acc / CONST.SNAF_M * Math.cos(ang);
        tmp.vy += acc / CONST.SNAF_M * Math.sin(ang);
        for(int i=0;i<AIParams.FLIPENDO_LOOP;i++)
            tmp.move();

        return Util.isIntersect(Global.opGoalX, CONST.POLL_LOWER + AIParams.FLIP_PASS_GOAL_BUFFER,
                                Global.opGoalX, CONST.POLL_UPPER - AIParams.FLIP_PASS_GOAL_BUFFER,
                                snaf.x, snaf.y,
                                tmp.x,tmp.y);
    }

    private static void updateStateForPetAndAddStateList(List<State> newStates, State baseState, int myMagic, int wizNum) {
        if (CONST.cPetrificus < myMagic &&
                (Global.lastCastTurn + AIParams.RECAST_TURN) < Global.turnCount ) {
            for (int i = 0; i < baseState.snaffles.size(); i++) {
                if (Util.getSpeed(baseState.snaffles.get(i)) < AIParams.PETRIF_SPEED)
                    continue;
//                if (Math.abs(baseState.snaffles.get(i).x - (double) Global.myGoalX) > AIParams.PETRIF_RANGE_X)
//                    continue;
                if( Util.checkNextTurnIsOut(baseState.snaffles.get(i)) )
                    continue;

                State tmp = baseState.clone();
                Snaffle targetSnaf = baseState.snaffles.get(i);

                tmp.snaffles.get(i).vx = 0;
                tmp.snaffles.get(i).vy = 0;
                tmp.myMagic -= CONST.cPetrificus;
                if (tmp.firstCommand.get(wizNum).equals(""))
                    tmp.firstCommand.set(wizNum, "PETRIFICUS " + targetSnaf.entityId);
                newStates.add(tmp);
            }
        }
    }

    static void simulateTurnAndEvaluate(State s) {
        State before = s.clone();

        addCommandToOpWizards(s);

        s.turnCount++;
        s.generation++;

        s.wizards.forEach(Wizard::move);
        s.opWizards.forEach(Thing::move);
        s.snaffles.forEach(Snaffle::move);
        s.bludgers.forEach(Thing::move);

        checkWizardGetSnaffle(before, s);
        s.snaffles.removeIf((snaf) -> {
            if(snaf.x < CONST.FIELD_Xmin){
                if(Global.myTeamId==0){
                    s.opScore++;
                }else{
                    s.myScore++;
                }
                return true;
            }else if(snaf.x > CONST.FIELD_Xmax){
                if(Global.myTeamId==0){
                    s.myScore++;
                }else{
                    s.opScore++;
                }
                return true;
            }
            return false;
        } );
        s.setScore();
    }

    static void addCommandToOpWizards(State s){
        if(s.snaffles.size()==0)
            return;

        for(Thing opWiz : s.opWizards){
            if(opWiz.state==0) {
                Thing closest = null;
                double minDist = Double.MAX_VALUE;
                for (Thing snaf : s.snaffles) {
                    double dist = Util.getDistance(opWiz, snaf);
                    if (minDist > dist) {
                        closest = snaf;
                        minDist = dist;
                    }
                }
                double rad = Util.getRadianAngle(opWiz, closest);
                opWiz.vx += CONST.WIZ_THRUST / CONST.WIZ_M * Math.cos(rad);
                opWiz.vy += CONST.WIZ_THRUST / CONST.WIZ_M * Math.sin(rad);

            }else{
                Snaffle snaf = (Snaffle) Util.getClosestThing(opWiz,s.snaffles);
                double rad=Util.getRadianAngle(snaf,Global.myGoal);
                snaf.vx = CONST.THROW_POWER / CONST.SNAF_M * Math.cos(rad);
                snaf.vy = CONST.THROW_POWER / CONST.SNAF_M * Math.sin(rad);
            }
        }
    }

    static void checkWizardGetSnaffle(State before, State after) {
        //自分
        for (int i = 0; i < before.wizards.size(); i++) {
            for (int j = 0; j < before.snaffles.size(); j++) {
                if (Util.isIntersect((long) before.wizards.get(i).x, (long) before.wizards.get(i).y,
                        (long) after.wizards.get(i).x, (long) after.wizards.get(i).y,
                        (long) before.snaffles.get(j).x, (long) before.snaffles.get(j).y,
                        (long) after.snaffles.get(j).x, (long) after.snaffles.get(j).y)) {
                    after.snaffles.get(j).x = after.wizards.get(i).x;
                    after.snaffles.get(j).vx = after.wizards.get(i).vx;
                    after.snaffles.get(j).y = after.wizards.get(i).y;
                    after.snaffles.get(j).vy = after.wizards.get(i).vy;
                    after.wizards.get(i).state = 1;
                    break;
                }
            }
        }
        //相手
        for (int i = 0; i < before.opWizards.size(); i++) {
            for (int j = 0; j < before.snaffles.size(); j++) {
                if (Util.isIntersect((long) before.opWizards.get(i).x, (long) before.opWizards.get(i).y,
                        (long) after.opWizards.get(i).x, (long) after.opWizards.get(i).y,
                        (long) before.snaffles.get(j).x, (long) before.snaffles.get(j).y,
                        (long) after.snaffles.get(j).x, (long) after.snaffles.get(j).y)) {
                    after.snaffles.get(j).x = after.opWizards.get(i).x;
                    after.snaffles.get(j).vx = after.opWizards.get(i).vx;
                    after.snaffles.get(j).y = after.opWizards.get(i).y;
                    after.snaffles.get(j).vy = after.opWizards.get(i).vy;
                    after.opWizards.get(i).state = 1;
                    break;
                }
            }
        }
    }
}

class State implements Cloneable, Comparable {
    public List<Wizard> wizards;
    public List<Thing> opWizards;
    public List<Snaffle> snaffles;
    public List<Thing> bludgers;
    public int myScore;
    public int myMagic;
    public int opScore;
    public int opMagic;
    public int turnCount;
    public int generation;
    public double score;
    public List<String> firstCommand;  // 毎ターン、魔法使いに与えるコマンドを出力する必要がある
    // コマンドはMOVE x y thrust, THROW x y power, FLIPENDO idなど

    public State(List<Wizard> wizards,
                 List<Thing> opWizards,
                 List<Snaffle> snaffles,
                 List<Thing> bludgers,
                 int myScore, int myMagic, int opScore, int opMagic, int turnCount, int generation) {
        this.wizards = new ArrayList<>();
        wizards.forEach((w) -> this.wizards.add(w.clone()));

        this.opWizards = new ArrayList<>();
        opWizards.forEach((ow) -> this.opWizards.add(ow.clone()));

        this.snaffles = new ArrayList<>();
        snaffles.forEach((s) -> this.snaffles.add(s.clone()));

        this.bludgers = new ArrayList<>();
        bludgers.forEach((b) -> this.bludgers.add(b.clone()));

        this.myScore = myScore;
        this.myMagic = myMagic;
        this.opScore = opScore;
        this.opMagic = opMagic;
        this.turnCount = turnCount;
        this.generation = generation;
        this.firstCommand = new ArrayList<>();  //空ならまだ１手も動かしていない
        this.firstCommand.add("");
        this.firstCommand.add("");
        //clone時にはshallow copyされる
        this.score = 0;
    }

    void setScore() {
        //数が少なくなるとsnafを動かす気が少なくなるのでへんだが、とりあえずやってみよう！
        final double[] distToOpGoalReward = {0}; //Weight * snaffle
        final double[] distToMyGoalPenalty = {0};
        this.snaffles.forEach((s) -> {
            distToOpGoalReward[0]+= Math.pow(AIParams.SNAF_DIST2OPGOAL_WEIGHT,
                            (CONST.FIELD_MAX_DIST-Util.getDistance(s,Global.opGoal))/CONST.FIELD_Xmax);
            distToMyGoalPenalty[0] += Math.pow(AIParams.SNAF_DIST2MYGOAL_WEIGHT,
                            (CONST.FIELD_MAX_DIST-Util.getDistance(s,Global.myGoal))/CONST.FIELD_Xmax);
        });
        /* やっぱ平均はだめ
        final double[] meanSnafDistToOpGoal = {0};
        final double[] meanSnafDistToMyGoal = {0};*/

        final double[] sumDistWiz2Snaf = {0};
        this.wizards.forEach(sumDist2ClosestSnaf(sumDistWiz2Snaf));

        final double[] sumDistOpWiz2Snaf = {0};
        this.opWizards.forEach(sumDist2ClosestSnaf(sumDistOpWiz2Snaf));

        /*final int[] countDangerSnaffles = {0};
        this.snaffles.forEach((s) -> {
            if (Math.abs(s.x - (double) Global.myGoalX) < AIParams.SNAF_PENALTY_RANGE_X) countDangerSnaffles[0]++;
        });*/

        //Score, wiz間の距離, snafのゴールまでの距離, wizとsnafの距離, opwizとsnafの距離
        this.score = AIParams.SCORE_WEIGHT * (myScore - opScore)
                + AIParams.WIZ_X_DIST_WEIGHT * Math.abs(wizards.get(0).x-wizards.get(1).x)//Util.getDistance(wizards.get(0), wizards.get(1))  //魔法使いの間は広いほど良い
                + AIParams.WIZ_Y_DIST_WEIGHT * Math.abs(wizards.get(0).y-wizards.get(1).y)//Util.getDistance(wizards.get(0), wizards.get(1))  //魔法使いの間は広いほど良い
                + distToOpGoalReward[0]  //snaffleがゴールに近いほど良い
                - distToMyGoalPenalty[0]
                - AIParams.WIZ2SNAF_DIST_WEIGHT * sumDistWiz2Snaf[0]
                + AIParams.OPWIZ2SNAF_DIST_WEIGHT * sumDistOpWiz2Snaf[0]
                + AIParams.MAGIC_WEIGHT * myMagic;
               //- AIParams.DENGER_SNAF_PENALTY * countDangerSnaffles[0];
    }

    void descScore() {
        final double[] distToOpGoalReward = {0}; //Weight * snaffle
        final double[] distToMyGoalPenalty = {0};
        this.snaffles.forEach((s) -> {
            distToOpGoalReward[0]+= Math.pow(AIParams.SNAF_DIST2OPGOAL_WEIGHT,
                    (CONST.FIELD_MAX_DIST-Util.getDistance(s,Global.opGoal))/CONST.FIELD_Xmax);
            distToMyGoalPenalty[0] += Math.pow(AIParams.SNAF_DIST2MYGOAL_WEIGHT,
                    (CONST.FIELD_MAX_DIST-Util.getDistance(s,Global.myGoal))/CONST.FIELD_Xmax);
        });

        final double[] sumDistWiz2Snaf = {0};
        this.wizards.forEach(sumDist2ClosestSnaf(sumDistWiz2Snaf));

        final double[] sumDistOpWiz2Snaf = {0};
        this.opWizards.forEach(sumDist2ClosestSnaf(sumDistOpWiz2Snaf));

        System.err.println("Pscore:" + AIParams.SCORE_WEIGHT * (myScore - opScore));
        System.err.println("POpGoalReward:"+distToOpGoalReward[0]);
        System.err.println("PMyGoalPenalty:"+distToMyGoalPenalty[0]);
        System.err.println("Pwizdist:" + AIParams.WIZ_X_DIST_WEIGHT * Math.abs(wizards.get(0).x-wizards.get(1).x));//Util.getDistance(wizards.get(0), wizards.get(1)));
        System.err.println("Pwiz2snaf:" + AIParams.WIZ2SNAF_DIST_WEIGHT * sumDistWiz2Snaf[0]);
        System.err.println("Popwiz2snaf:" + AIParams.OPWIZ2SNAF_DIST_WEIGHT * sumDistOpWiz2Snaf[0]);
        System.err.println("Pmagic:"+ AIParams.MAGIC_WEIGHT * myMagic);
    }

    private Consumer<Thing> sumDist2ClosestSnaf(double[] sumDistWiz2Snaf) {
        return (w) -> {
            Snaffle s = (Snaffle) Util.getClosestThing(w, snaffles);
            sumDistWiz2Snaf[0] += s == null ? 0 : Util.getDistance(w, s);
        };
    }

    @Override
    public int compareTo(Object o) {
        State other = (State) o;
        /*if (this.score == other.score) {
            if (this.equals(o)) {
                return 0;
            } else {
                return (int) (this.time - other.time);
            }
        }*/
        return -(int) (this.score - other.score); //Descending order
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State)) return false;

        State state = (State) o;
        if (myScore != state.myScore) return false;
        if (myMagic != state.myMagic) return false;
        if (opScore != state.opScore) return false;
        if (opMagic != state.opMagic) return false;
        if (turnCount != state.turnCount) return false;
        if (Double.compare(state.score, score) != 0) return false;
        if (!wizards.equals(state.wizards)) return false;
        if (!opWizards.equals(state.opWizards)) return false;
        if (!snaffles.equals(state.snaffles)) return false;
        if (!bludgers.equals(state.bludgers)) return false;
        return firstCommand.equals(state.firstCommand);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = wizards.hashCode();
        result = 31 * result + opWizards.hashCode();
        result = 31 * result + snaffles.hashCode();
        result = 31 * result + bludgers.hashCode();
        result = 31 * result + myScore;
        result = 31 * result + myMagic;
        result = 31 * result + opScore;
        result = 31 * result + opMagic;
        result = 31 * result + turnCount;
        temp = Double.doubleToLongBits(score);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        result = 31 * result + firstCommand.hashCode();
        return result;
    }

    @Override
    protected State clone() {
        try {
            State tmp = (State) super.clone();  //shallow copyなのでObject型のコピーは書く必要がある

            //致し方なし
            tmp.wizards = new ArrayList<>();
            this.wizards.forEach((wiz) -> tmp.wizards.add(wiz.clone()));

            tmp.opWizards = new ArrayList<>();
            this.opWizards.forEach((opWiz) -> tmp.opWizards.add(opWiz.clone()));

            tmp.snaffles = new ArrayList<>();
            this.snaffles.forEach((snaf) -> tmp.snaffles.add(snaf.clone()));

            tmp.bludgers = new ArrayList<>();
            this.bludgers.forEach((bludg) -> tmp.bludgers.add(bludg.clone()));

            tmp.firstCommand = new ArrayList<>(this.firstCommand);

            return tmp;
        } catch (CloneNotSupportedException e) {
            System.out.println(e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @Override
    public String toString() {
        return "State{" +
                "wizards=" + wizards.toString() +
                ", opWizards=" + opWizards.toString() +
                ", snaffles=" + snaffles.toString() +
                ", bludgers=" + bludgers.toString() +
                ", myScore=" + myScore +
                ", myMagic=" + myMagic +
                ", opScore=" + opScore +
                ", opMagic=" + opMagic +
                ", firstCommand=" + firstCommand +
                '}';
    }
}


class Wizard extends Thing implements Cloneable {
    int runPower = 150;
    int throwPower = 500;

    public Wizard() {
    }

    public Wizard(Thing t) {
        super(t.x, t.y, t.vx, t.vy, t.state, t.entityId, t.entityType);
    }

    @Override
    public Wizard clone() {
        return (Wizard) super.clone();
    }

}

class OpWizard extends Thing implements Cloneable {

    @Override
    protected OpWizard clone() {
        return (OpWizard) super.clone();
    }

}

class Snaffle extends Thing implements Cloneable {
    public Snaffle() {
    }

    public Snaffle(Thing t) {
        super(t.x, t.y, t.vx, t.vy, t.state, t.entityId, t.entityType);
    }

    @Override
    public String toString() {
        return "Snaffle" + super.toString();
    }

    @Override
    protected Snaffle clone() {
        return (Snaffle) super.clone();
    }
}

class Thing implements Cloneable {
    public Thing() {
    }

    public Thing(double x, double y, double vx, double vy, int state, int entityId, String entityType) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.state = state;
        this.entityId = entityId;
        this.entityType = entityType;
        switch (this.entityType) {
            case "WIZARD":
                this.r = CONST.WIZ_R;
                this.m = CONST.WIZ_M;
                this.f = CONST.WIZ_F;
                break;
            case "OPPONENT_WIZARD":
                this.r = CONST.WIZ_R;
                this.m = CONST.WIZ_M;
                this.f = CONST.WIZ_F;
                break;
            case "SNAFFLE":
                this.r = CONST.SNAF_R;
                this.m = CONST.SNAF_M;
                this.f = CONST.SNAF_F;
                break;
            case "BLUDGER":
                this.r = CONST.BLUD_R;
                this.m = CONST.BLUD_M;
                this.f = CONST.BLUD_F;
                break;
            default:
                this.r = 1;
                this.m = 1;
                this.f = 1;
        }
    }

    /**
     * movement of entities is computed as following
     * 1. Thrust
     * 2. Spell power
     * 3. Movement
     * 4. Friction
     * 5. Rounding
     */
    //Thingではシミュレーションのうち3,4,5を担当する。1,2の加速処理はPlayerが担当する
    //x, vはdoubleだが加速時とこのメソッドないでしか小数部は出ない
    public void move() {
        //3, 5
        this.x = this.x + this.vx;
        this.y = this.y + this.vy;

        //calculate collisions to field line
        bound();

        //4
        this.vx *= this.f;
        this.vy *= this.f;

        //5
        this.x = Math.round(this.x);
        this.y = Math.round(this.y);
        this.vx = Math.round(this.vx);
        this.vy = Math.round(this.vy);
    }

    public void bound() {
        //Bound
        int checkYmin = CONST.FIELD_Ymin + this.r;
        int checkYmax = CONST.FIELD_Ymax - this.r;

        if (this.y < checkYmin) {
            this.y = checkYmin + (checkYmin - this.y); //y==0 で折り返してrを足す
            this.vy = -this.vy;
        } else if (this.y > checkYmax) {
            this.y = checkYmax - (this.y - checkYmax);
            this.vy = -this.vy;
        }

        int checkXmin = CONST.FIELD_Xmin + this.r;
        int checkXmax = CONST.FIELD_Xmax - this.r;

        if (!((CONST.POLL_LOWER + this.r + CONST.SIM_GOAL_BUF) < y &&
                (y < (CONST.POLL_UPPER - this.r - CONST.SIM_GOAL_BUF)))) {
            if (this.x < checkXmin) {
                this.x = checkXmin + (checkXmin - this.x); //x==0 で折り返してrを足す
                this.vx = -this.vx;
            } else if (this.x > (CONST.FIELD_Xmax - this.r)) {
                this.x = checkXmax - (this.x - checkXmax);
                this.vx = -this.vx;
            }
        }
    }

    double x;
    double y;
    double vx;
    double vy;
    int state;
    int entityId;
    String entityType;
    int r;
    double m;
    double f;

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

    @Override
    protected Thing clone() {
        try {
            return (Thing) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}



class Util {
    //-----幾何関係-----
    //AB to CD が交差してるか返す
    //http://qiita.com/ykob/items/ab7f30c43a0ed52d16f2
    static boolean isIntersect(long ax, long ay, long bx, long by, long cx, long cy, long dx, long dy) {
        long ta = (cx - dx) * (ay - cy) + (cy - dy) * (cx - ax);
        long tb = (cx - dx) * (by - cy) + (cy - dy) * (cx - bx);
        long tc = (ax - bx) * (cy - ay) + (ay - by) * (ax - cx);
        long td = (ax - bx) * (dy - ay) + (ay - by) * (ax - dx);
        return tc * td < 0 && ta * tb < 0;
        /*boolean flag = tc * td < 0 && ta * tb < 0;
        if (flag) {
            System.err.println(ta + "," + tb + "," + tc + "," + td);
            System.err.println("(" + ax + "," + ay + "," + bx + "," + by + ")(" + cx + "," + cy + "," + dx + "," + dy + ") insersects.");
        }

        return flag;*/
    }

    static boolean isIntersect(double ax, double ay, double bx, double by, double cx, double cy, double dx, double dy) {
        double ta = (cx - dx) * (ay - cy) + (cy - dy) * (cx - ax);
        double tb = (cx - dx) * (by - cy) + (cy - dy) * (cx - bx);
        double tc = (ax - bx) * (cy - ay) + (ay - by) * (ax - cx);
        double td = (ax - bx) * (dy - ay) + (ay - by) * (ax - dx);
        return tc * td < 0 && ta * tb < 0;
        /*boolean flag = tc * td < 0 && ta * tb < 0;
        if (flag) {
            System.err.println(ta + "," + tb + "," + tc + "," + td);
            System.err.println("(" + ax + "," + ay + "," + bx + "," + by + ")(" + cx + "," + cy + "," + dx + "," + dy + ") insersects.");
        }

        return flag;*/
    }


    //round half away from zero: ゼロから遠い方へ丸める
    //http://www.ftext.org/text/subsubsection/2365
    static long getExtendedPoint(long ax, long ay, long bx, long by) {
        long goalx = Global.opGoalX;
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
        double l = getDistance(fromX, fromY, toX, toY);
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

    static double getDistance(long x1, long y1, long x2, long y2) {
        long diffx = x2 - x1;
        long diffy = y2 - y1;
        return Math.sqrt(diffx * diffx + diffy * diffy);
    }

    static double getDistance(Thing from, Thing to) {
        return Math.sqrt((to.x - from.x) * (to.x - from.x) + (to.y - from.y) * (to.y - from.y));
    }

    static double getSpeed(Thing t) {
        return Math.sqrt(t.vx * t.vx + t.vy * t.vy);
    }

    //fromからtoへの角度を返す。±180度をradに変換したものが帰ってくる
    static double getRadianAngle(Thing from, Thing to) {
        return Math.atan2(to.y - from.y, to.x - from.x);
    }

    //-------------その他
    static void setCenterOfGoal() {
        Global.myGoalX = Global.myTeamId == 0 ? 0 : 16000;
        Global.myGoalY = 3750;
        Global.opGoalX = Global.myTeamId == 0 ? 16000 : 0;
        Global.opGoalY = 3750;

        Global.myGoal = new Thing(Global.myGoalX, Global.myGoalY,
                0.0,0.0, 0, -1,"" );
        Global.opGoal = new Thing(Global.opGoalX, Global.opGoalY,
                0.0,0.0, 0, -2,"" );
    }

    /*static double getFriction(String entityType) {
        switch (entityType) {
            case "WIZARD":
                return CONST.WIZ_F;
            case "OPPONENT_WIZARD":
                return CONST.WIZ_F;
            case "SNAFFLE":
                return CONST.SNAF_F;
            case "BLUDGER":
                return CONST.BLUD_F;
            default:
                return 1.0;
        }
    }*/

    static int getMoveTargetDiffX(double rad) {
        return (int) Math.round(CONST.WIZ_DEST_R * Math.cos(rad));
    }

    static int getMoveTargetDiffY(double rad) {
        return (int) Math.round(CONST.WIZ_DEST_R * Math.sin(rad));
    }

    static double distMyGoal(Thing t) {
        return Math.abs(Global.myGoalX - t.x);
    }

    static double dist2EnemyGoal(Thing t) {
        return Math.abs(Global.opGoalX - t.x);
    }

    static boolean isGoingToMyGoal(Thing t) {
        return Global.myTeamId == 0 ? t.vx < 0 : t.vx > 0;
    }

    static boolean isGoingToEnemyGoal(Thing t) {
        return Global.myTeamId == 0 ? t.vx > 0 : t.vx < 0;
    }

    static boolean outOfField(long x, long y) {
        return !(CONST.FIELD_Xmin < x && x < CONST.FIELD_Xmax &&
                CONST.FIELD_Ymin < y && y < CONST.FIELD_Ymax);
    }
    static boolean outOfField(double x, double y) {
        return !(CONST.FIELD_Xmin < x && x < CONST.FIELD_Xmax &&
                CONST.FIELD_Ymin < y && y < CONST.FIELD_Ymax);
    }

    static boolean inField(long x, long y) {
        return (CONST.FIELD_Xmin < x && x < CONST.FIELD_Xmax &&
                CONST.FIELD_Ymin < y && y < CONST.FIELD_Ymax);
    }

    static double getMinDist2Enemies(Thing t, Map<Integer, Thing> opWizards) {
        double minDist = Double.MAX_VALUE;
        for (Map.Entry<Integer, Thing> e : opWizards.entrySet()) {
            double d = getDistance(t, e.getValue());
            if (minDist > d) {
                minDist = d;
            }
        }
        return minDist;
    }

    //アップキャストして使えるよ　Thingは参照で渡します
    static Thing getClosestThing(Thing t, List<? extends Thing> list) {
        Thing closest = null;
        double minDist = Double.MAX_VALUE;

        //if (list.isEmpty()) System.err.println("getClosestThing list is empty");

        for (Thing tmp : list) {
            double dist2tmp = Util.getDistance(t, tmp);
            if (minDist > dist2tmp) {
                minDist = dist2tmp;
                closest = tmp;
            }
        }
        return closest;
    }

    //base: filpendo-> 6000, accio-> 3000
    static double getSpellAcc(double dist, double basePower) {
        double tmp = dist / 1000.0;
        return Math.min(basePower / (tmp * tmp), 1000);
    }

    static boolean checkNextTurnIsOut(Snaffle s){
        Snaffle tmp = s.clone();
        tmp.move();
        return Util.outOfField(tmp.x,tmp.y);
    }

}

class Global {

    static int turnCount = 0;
    static int usedSpellCost = 0;
    static int myTeamId = 0;
    static int lastCastTurn = 0;

    static int opGoalX;
    static int opGoalY;

    static int myGoalX;
    static int myGoalY;

    static Thing myGoal;
    static Thing opGoal;
}

class CONST {
    static double WIZ_F = 0.75;
    static double SNAF_F = 0.75;
    static double BLUD_F = 0.9;

    static int SNAF_R = 150;
    static int WIZ_R = 400;
    static int BLUD_R = 200;

    static double SNAF_M = 0.5;
    static double WIZ_M = 1;
    static double BLUD_M = 8;

    static int cFlipendo = 20;
    static int cAccio = 20;
    static int cPetrificus = 10;
    static int cObliviate = 5;

    static double FLIPENDO_POWER = 6000.0;
    static double ACCIO_POWER = 3000.0;

    static int MANA_BUFFER = 10;

    static int[] SPELL_COST = {20, 20, 10, 5}; //Flipend, Accio, Petrificus, Obliviate
    static String[] SPELL_STR = {"FLIPENDO", "ACCIO", "PETRIFICUS", "OBLIVIATE"};

    static int POLL_LOWER = 2050;   //center - 2000(=goalWidth/2) + 300(=r)
    static int POLL_UPPER = 5450;

    static int FIELD_Xmin = 0;
    static int FIELD_Xmax = 16000;
    static int FIELD_Ymin = 0;
    static int FIELD_Ymax = 7500;

    static int WIZ_THRUST = 150;
    static int THROW_POWER = 500;

    private static int DEGREE_DIFF = 15;

    static double DIST2GOAL_BASE = 7500;

    static Integer[] MOVE_X;
    static Integer[] MOVE_Y;
    static int WIZ_DEST_R = 400;
    static Double[] RADIANS;

    static int SIM_GOAL_BUF = 100;

    static List<String> dummyCommand = new ArrayList<>();

    static double FIELD_MAX_DIST = Util.getDistance(0,0,CONST.FIELD_Xmax,CONST.FIELD_Ymax);

    static {
        dummyCommand.add("MOVE 7500 3500 150");
        dummyCommand.add("MOVE 7500 3500 150");


        List<Integer> x = new ArrayList<>();
        List<Integer> y = new ArrayList<>();
        List<Double> rads = new ArrayList<>();

        for (int i = 0; i < 360; i += DEGREE_DIFF) {
            rads.add(Math.toRadians(i));
            x.add((int) Math.round(WIZ_DEST_R * Math.cos(Math.toRadians(i))));
            y.add((int) Math.round(WIZ_DEST_R * Math.sin(Math.toRadians(i))));
        }
        MOVE_X = x.toArray(new Integer[0]);
        MOVE_Y = y.toArray(new Integer[0]);
        RADIANS = rads.toArray(new Double[0]);
    }
}

class AIParams {
    static int SCORE_WEIGHT = 500000;
    static double WIZ_X_DIST_WEIGHT = 0.2;//0.33;//wiz間の距離に欠けるときは0.2がいい感じだった;
    static double WIZ_Y_DIST_WEIGHT = 0.1;//0.15;//wiz間の距離に欠けるときは0.2がいい感じだった;
    static double SNAF_DIST2OPGOAL_WEIGHT = 60000;
    static double SNAF_DIST2MYGOAL_WEIGHT = 40000;

    static double OPWIZ_CLOSE_DIST = 800;

    static double WIZ2SNAF_DIST_WEIGHT = 0.5;
    static double OPWIZ2SNAF_DIST_WEIGHT = 0.1;// 0.2

    static double MAGIC_WEIGHT = 210;

    static double SNAF_PENALTY_RANGE_X = 2500;
    static double DENGER_SNAF_PENALTY = 5000;

    static double FLIP_DIST_MAX = 3000;
    static double ACCIO_DIST_MAX = 3000;

    static double PETRIF_RANGE_X = 6500;
    static double PETRIF_SPEED = 1200; //投げたときの最大速度が500/0.5=1000

    static int BEAM_WIDTH = 110;

    static int SEARCH_DURATION = 90;

    static int FLIPENDO_LOOP = 10;

    static int RECAST_TURN = 2;

    static int FLIP_PASS_GOAL_BUFFER = 400;
}

