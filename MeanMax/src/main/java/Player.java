import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 入出力関連の処理を行う
 */
class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);

        // game loop
        while (true) {
            int myScore = in.nextInt();
            int enemyScore1 = in.nextInt();
            int enemyScore2 = in.nextInt();
            int myRage = in.nextInt();
            int enemyRage1 = in.nextInt();
            int enemyRage2 = in.nextInt();
            int unitCount = in.nextInt();

            Reaper myReaper = null;
            Destroyer myDestroyer = null;
            List<Reaper> opReapers = new ArrayList<>();
            List<Wreck> wrecks = new ArrayList<>();
            List<Destroyer> opDestroyers = new ArrayList<>();
            List<Tanker> tankers = new ArrayList<>();

            for (int i = 0; i < unitCount; i++) {
                int unitId = in.nextInt();
                int unitType = in.nextInt();
                int player = in.nextInt();
                float mass = in.nextFloat();
                int radius = in.nextInt();
                int x = in.nextInt();
                int y = in.nextInt();
                int vx = in.nextInt();
                int vy = in.nextInt();
                int water = in.nextInt();
                int extra2 = in.nextInt();
                /*System.err.println("unitId:" + String.valueOf(unitId));
                System.err.println("unitType:" + String.valueOf(unitType));
                System.err.println("player:" + String.valueOf(player));*/

                switch (unitType) {
                    case 0: //reaper
                        Reaper r = new Reaper(unitId, null, x, y, radius);
                        if (player == 0) {
                            myReaper = r;
                        } else if (player == -1) {

                        } else {
                            opReapers.add(r);
                        }
                        break;
                    case 1: //Destroyer
                        Destroyer d = new Destroyer(unitId, null, x, y, radius);
                        if (player == 0) {
                            myDestroyer = d;
                        } else if (player == -1) {

                        } else {
                            opDestroyers.add(d);
                        }
                        break;
                    case 3: //Tanker
                        Tanker t = new Tanker(unitId, x, y, null, radius);
                        tankers.add(t);
                        break;
                    case 4: //wreck
                        Wreck w = new Wreck(x, y, water, radius);
                        wrecks.add(w);
                        break;
                }
            }

//            System.err.println(String.format("nW, nT: %s %s", wrecks.size(), tankers.size()));
//            String moveR = moveReaper(myReaper, wrecks, tankers);
//            String moveD = moveDestroyer(myDestroyer, myReaper, tankers);
//            System.out.println(moveR + " " + moveR);
//            System.out.println(moveD + " " + moveD);
            System.out.println("WAIT");
        }
    }

    static <T extends Point> T getClosest(Reaper r, List<T> points) {
        double minDist = Double.MAX_VALUE;
        T res = null;
        for (T u : points) {
            double tmp = Util.getDistance(r, u);
            if (tmp < minDist) {
                minDist = tmp;
                res = u;
            }
        }
        return res;
    }

    static <T extends Point> T getInFieldClosest(Reaper r, List<T> points, double rangeFromCenter) {
        List<T> res = new ArrayList<>();
        for (T p : points) {
            if (Util.getDistance(p, CONST.CENTER) < rangeFromCenter)
                res.add(p);
        }
        return getClosest(r, res);
    }


    static String moveReaper(Reaper r, List<Wreck> wrecks, List<Tanker> tankers) {
        Wreck w = getClosest(r, wrecks);
        Tanker t = getInFieldClosest(r, tankers, 6500);

        if (w == null) {
            if (t == null) {
                return "WAIT";
            }
            return Util.getOutput(t.x, t.y,
                    Util.getRanged(Util.getDistance(r, t) / 3500 * 300, 100, 300));
        }

        double dist = Util.getDistance(r, w);

        if (w.water > 1) {
            return Util.getOutput(w.x, w.y,
                    Util.getRanged(dist / 3500 * 300, 100, 200));
        }

        /*if(w.water > 1 && dist < w.radius) {
            System.err.println(String.format("reaper x y: %s %s", (int)r.x, (int)r.y));
            System.err.println(String.format("wreck x y rad/ %s %s %s %s", (int)w.x, (int)w.y, w.radius, dist));
            return "WAIT";
        }*/
        System.err.println(String.format("target: %s %s", w.radius, w.water));
        return Util.getOutput(w.x, w.y,
                Util.getRanged(dist / 3500 * 300, 100, 200));
    }

    static String moveDestroyer(Destroyer d, Reaper r, List<Tanker> tankers) {

        Tanker t = getInFieldClosest(r, tankers, 7000);
        if (t == null) {
            double dist = Util.getDistance(d, r);
            return Util.getOutput(r.x, r.y, 300);
            //Util.getRanged(dist/ 300 * 300, 50, 300));
        }
        double dist = Util.getDistance(r, t);
        return Util.getOutput(t.x, t.y, 300);
        //Util.getRanged(dist / 2000 * 300, 100, 300));
    }

    static List<String> solve(long startTime, State startState, Evaluator evaluator) {

        long currentTime = System.currentTimeMillis();
        startState.stateScore = evaluator.eval(startState);
        State bestState = startState.clone();

        TreeSet<State> beam1 = new TreeSet<>();
        TreeSet<State> beam2 = new TreeSet<>();
        beam1.add(startState.clone());
        //System.err.println("beam1:" + beam1.first().toString());
        int turnsSimulated = 0;
        while (currentTime < (startTime + AIParam.SEARCH_MSEC)) {  //何を答えとするか？ => 読める中で一番良い盤面になる状態に繊維する

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

        System.err.println("simulated turn: " + turnsSimulated);
        //System.err.println("bestState: " + bestState.score + " " + bestState.firstCommand);

        return bestState.firstCommand;
    }

    static State updateQueue(State now, State bestState, TreeSet<State> queue, long limitTime) {

        //全方向移動・投球　または　魔法
        for (int dir0 = 0; dir0 <= CONST.RADIANS.length; dir0++) { //i==lenで使えるなら魔法を使う。snaffleを持っているなら必ず投げる
            for (int dir1 = 0; dir1 <= CONST.RADIANS.length; dir1++) {

            }

            if (System.currentTimeMillis() > limitTime)
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

        Wizard targetWiz = tmp.wizards.get(wizNum);
        int destX = (int) targetWiz.x + Util.getMoveTargetDiffX(CONST.RADIANS[direction]);
        int destY = (int) targetWiz.y + Util.getMoveTargetDiffY(CONST.RADIANS[direction]);

        if (!Util.inField(destX, destY)) {
            //System.err.println("break dest x:" + destX + " y:" + destY);
            return true;
        }

        if (targetWiz.state == 0) { //snaffleを持っていなければ移動
            targetWiz.vx += CONST.WIZ_THRUST / CONST.WIZ_M * Math.cos(CONST.RADIANS[direction]);
            targetWiz.vy += CONST.WIZ_THRUST / CONST.WIZ_M * Math.sin(CONST.RADIANS[direction]);

            if (tmp.firstCommand.get(wizNum).equals(""))
                tmp.firstCommand.set(wizNum, "MOVE " + destX + " " + destY + " " + CONST.WIZ_THRUST);

        } else {
            Snaffle throwTarget = (Snaffle) Util.getClosestThing(targetWiz, tmp.snaffles);

            if (throwTarget == null) return false;

            throwTarget.vx += CONST.THROW_POWER / CONST.SNAF_M * Math.cos(CONST.RADIANS[direction]);
            throwTarget.vy += CONST.THROW_POWER / CONST.SNAF_M * Math.sin(CONST.RADIANS[direction]);
            if (tmp.firstCommand.get(wizNum).equals(""))
                tmp.firstCommand.set(wizNum, "THROW " + destX + " " + destY + " " + CONST.THROW_POWER);
        }
        return false;
    }

    //spellName: FLIPENDO or ACCIO, wizardId: 0 or 1
    private static void updateStateForSpellAndAddStateList(List<State> newStates, State baseState, String spellName, int spellCost, double spellPower, int myMagic, int wizNum) {
        if (spellCost < myMagic) {
            for (int i = 0; i < baseState.snaffles.size(); i++) { //Flipendoを打つのはsnaffleに対してのみ
                State tmp = baseState.clone();
                Snaffle tmpTargetSnaf = baseState.snaffles.get(i);
                Wizard targetWiz = tmp.wizards.get(wizNum);

                //accioなら180度回転
                double angle = spellName.equals("FLIPENDO") ? Util.getRadianAngle(targetWiz, tmpTargetSnaf)
                        : Util.getRadianAngle(tmpTargetSnaf, targetWiz);
                double deg = Math.toDegrees(angle);

                //効果ある方向に打つか
                if (-90 <= deg && deg <= 90) {
                    if (Global.myTeamId == 1)
                        continue;
                } else {
                    if (Global.myTeamId == 0)
                        continue;
                }

                double dist = Util.getDistance(targetWiz, tmpTargetSnaf);
                double acc = Util.getSpellAcc(dist, spellPower);

                if (Util.checkNext2TurnIsOut(tmp.snaffles.get(i)))
                    continue;

                //ACCIOなら遠すぎないかチェック
                if (spellName.equals("ACCIO") && (dist > AIParams.ACCIO_DIST_MAX || dist < AIParams.ACCIO_DIST_MIN))
                    continue;

                //FLIPENDOなら10ターン後に入るかチェック
                if (spellName.equals("FLIPENDO") &&
                        (dist > AIParams.FLIP_DIST_MAX || !willPassGoal(tmpTargetSnaf, acc, angle) || dist < AIParams.FLIP_DIST_MIN))
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

    private static boolean willPassGoal(Snaffle snaf, double acc, double ang) {
        Snaffle tmp = snaf.clone();
        tmp.vx += acc / CONST.SNAF_M * Math.cos(ang);
        tmp.vy += acc / CONST.SNAF_M * Math.sin(ang);
        for (int i = 0; i < AIParams.FLIPENDO_LOOP; i++)
            tmp.move();

        return Util.isIntersect(Global.opGoalX, CONST.POLL_LOWER + AIParams.FLIP_PASS_GOAL_BUFFER,
                Global.opGoalX, CONST.POLL_UPPER - AIParams.FLIP_PASS_GOAL_BUFFER,
                snaf.x, snaf.y,
                tmp.x, tmp.y);
    }

    private static void updateStateForPetAndAddStateList(List<State> newStates, State baseState, int myMagic, int wizNum) {
        if (CONST.cPetrificus < myMagic &&
                (Global.lastCastTurn + AIParams.RECAST_TURN) < Global.turnCount) {
            for (int i = 0; i < baseState.snaffles.size(); i++) {
                if (Util.getSpeed(baseState.snaffles.get(i)) < AIParams.PETRIF_SPEED)
                    continue;
//                if (Math.abs(baseState.snaffles.get(i).x - (double) Global.myGoalX) > AIParams.PETRIF_RANGE_X)
//                    continue;
                if (Util.checkNext2TurnIsOut(baseState.snaffles.get(i)))
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
            if (snaf.x < CONST.FIELD_Xmin) {
                if (Global.myTeamId == 0) {
                    s.opScore++;
                } else {
                    s.myScore++;
                }
                return true;
            } else if (snaf.x > CONST.FIELD_Xmax) {
                if (Global.myTeamId == 0) {
                    s.myScore++;
                } else {
                    s.opScore++;
                }
                return true;
            }
            return false;
        });
        s.setScore();
    }

    //最も近いsnaffleに近づくか、ゴールに向かって投げる
    static void addCommandToOpWizards(State s) {
        if (s.snaffles.size() == 0)
            return;

        for (Thing opWiz : s.opWizards) {
            if (opWiz.state == 0) {
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

            } else {
                Snaffle snaf = (Snaffle) Util.getClosestThing(opWiz, s.snaffles);
                double rad = Util.getRadianAngle(snaf, Global.myGoal);
                snaf.vx = CONST.THROW_POWER / CONST.SNAF_M * Math.cos(rad);
                snaf.vy = CONST.THROW_POWER / CONST.SNAF_M * Math.sin(rad);
            }
        }
    }
}

/**
 * 状態
 * TODO: Stateが作られるタイミングで移動とスコアリングを終えるようにする
 */
class State implements Comparable {
    int myScore;
    int enemyScore1;
    int enemyScore2;
    int myRage;
    int enemyRage1;
    int enemyRage2;

    double stateScore;

    Reaper myReaper;
    Destroyer myDestroyer;
    List<Reaper> opReapers;
    List<Destroyer> opDestroyers;
    List<Wreck> wrecks;
    List<Tanker> tankers;
    List<String> commands;

    public State(Reaper myReaper,
                 Destroyer myDestroyer,
                 List<Reaper> opReapers,
                 List<Destroyer> opDestroyers,
                 List<Wreck> wrecks,
                 List<Tanker> tankers,
                 List<String> commands) {
        this.myReaper = myReaper;
        this.myDestroyer = myDestroyer;
        this.opReapers = opReapers;
        this.opDestroyers = opDestroyers;
        this.wrecks = wrecks;
        this.tankers = tankers;
        this.commands = commands;
    }

    /**
     * https://vyazelenko.com/2013/10/30/clone-vs-copy-constructor-a-closer-look/
     * https://vyazelenko.com/2013/10/29/copy-object-in-java-performance-comparison/
     *
     * @return
     */
    @Override
    protected State clone() {
        try {
            State tmp = (State) super.clone(); //Object型以外のdeep copyを行う
            tmp.myReaper = this.myReaper.clone();
            tmp.myDestroyer = this.myDestroyer.clone();
            tmp.opReapers = this.opReapers.stream()
                    .map(Reaper::clone)
                    .collect(Collectors.toList());
            tmp.opDestroyers = this.opDestroyers.stream()
                    .map(Destroyer::clone)
                    .collect(Collectors.toList());
            tmp.wrecks = this.wrecks.stream()
                    .map(Wreck::clone)
                    .collect(Collectors.toList());
            tmp.tankers = this.tankers.stream()
                    .map(Tanker::clone)
                    .collect(Collectors.toList());
            tmp.commands = new ArrayList<>(this.commands); //stringはimmutable

            return tmp;
        } catch (CloneNotSupportedException e) {
            System.err.println(e.getMessage());
            throw new RuntimeException();
        }
    }

    @Override
    public int compareTo(@NotNull Object o) {
        return 0;
    }
}

/**
 * 位置
 */
class Point implements Cloneable {
    double x;
    double y;

    Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    double distance(Point p) {
        return Math.sqrt((this.x - p.x) * (this.x - p.x) + (this.y - p.y) * (this.y - p.y));
    }

    // Move the point to x and y
    void move(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // Move the point to an other point for a given distance
    void moveTo(Point p, double distance) {
        double d = distance(p);

        if (d < CONST.EPSILON) {
            return;
        }

        double dx = p.x - x;
        double dy = p.y - y;
        double coef = distance / d;

        this.x += dx * coef;
        this.y += dy * coef;
    }

    boolean isInRange(Point p, double range) {
        return p != this && distance(p) <= range;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Point other = (Point) obj;
        if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x)) return false;
        if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y)) return false;
        return true;
    }

    @Override
    protected Point clone() {
        try {
            return (Point) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}

/**
 * wreck
 */
class Wreck extends Point {
    int unitId;
    double radius;
    int water;
    boolean known;
    InnerPlayer innerPlayer;

    Wreck(double x, double y, int water, double radius) {
        super(x, y);

        this.radius = radius;
        this.water = water;
    }

    // Reaper harvesting
    public boolean harvest(List<InnerPlayer> innerPlayers, Set<SkillEffect> skillEffects) {
        innerPlayers.forEach(p -> {
            if (isInRange(p.getReaper(), radius) && !p.getReaper().isInDoofSkill(skillEffects)) {
                p.score += 1;
                water -= 1;
            }
        });

        return water > 0;
    }

    @Override
    protected Wreck clone() {
        return (Wreck) super.clone();
    }
}

/**
 * 物体の抽象クラス
 */
abstract class Unit extends Point {
    int unitType;
    int unitId;
    double vx;
    double vy;
    double radius;
    double mass;
    double friction;
    boolean known;

    Unit(int unitType, int unitId, double x, double y, double radius) {
        super(x, y);

        this.unitId = unitId;
        this.unitType = unitType;
        this.radius = radius;

        vx = 0.0;
        vy = 0.0;

        known = false;
    }

    void move(double t) {
        x += vx * t;
        y += vy * t;
    }

    double speed() {
        return Math.sqrt(vx * vx + vy * vy);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + unitId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Unit other = (Unit) obj;
        if (unitId != other.unitId)
            return false;
        return true;
    }

    void thrust(Point p, int power) {
        double distance = distance(p);

        // Avoid a division by zero
        if (Math.abs(distance) <= CONST.EPSILON) {
            return;
        }

        double coef = (((double) power) / mass) / distance;
        vx += (p.x - this.x) * coef;
        vy += (p.y - this.y) * coef;
    }

    public boolean isInDoofSkill(Set<SkillEffect> skillEffects) {
        return skillEffects.stream().anyMatch(s -> s instanceof DoofSkillEffect && isInRange(s, s.radius + radius));
    }

    void adjust(Set<SkillEffect> skillEffects) {
        x = Util.round(x);
        y = Util.round(y);

        if (isInDoofSkill(skillEffects)) {
            // No friction if we are in a doof skill effect
            vx = Util.round(vx);
            vy = Util.round(vy);
        } else {
            vx = Util.round(vx * (1.0 - friction));
            vy = Util.round(vy * (1.0 - friction));
        }
    }

    // Search the next collision with the map border
    Collision getCollision() {
        // Check instant collision
        if (distance(CONST.CENTER) + radius >= CONST.MAP_RADIUS) {
            return new Collision(0.0, this);
        }

        // We are not moving, we can't reach the map border
        if (vx == 0.0 && vy == 0.0) {
            return CONST.NULL_COLLISION;
        }

        // Search collision with map border
        // Resolving: sqrt((x + t*vx)^2 + (y + t*vy)^2) = MAP_RADIUS - radius <=> t^2*(vx^2 + vy^2) + t*2*(x*vx + y*vy) + x^2 + y^2 - (MAP_RADIUS - radius)^2 = 0
        // at^2 + bt + c = 0;
        // a = vx^2 + vy^2
        // b = 2*(x*vx + y*vy)
        // c = x^2 + y^2 - (MAP_RADIUS - radius)^2

        double a = vx * vx + vy * vy;

        if (a <= 0.0) {
            return CONST.NULL_COLLISION;
        }

        double b = 2.0 * (x * vx + y * vy);
        double c = x * x + y * y - (CONST.MAP_RADIUS - radius) * (CONST.MAP_RADIUS - radius);
        double delta = b * b - 4.0 * a * c;

        if (delta <= 0.0) {
            return CONST.NULL_COLLISION;
        }

        double t = (-b + Math.sqrt(delta)) / (2.0 * a);

        if (t <= 0.0) {
            return CONST.NULL_COLLISION;
        }

        return new Collision(t, this);
    }

    // Search the next collision with an other unit
    Collision getCollision(Unit u) {
        // Check instant collision
        if (distance(u) <= radius + u.radius) {
            return new Collision(0.0, this, u);
        }

        // Both units are motionless
        if (vx == 0.0 && vy == 0.0 && u.vx == 0.0 && u.vy == 0.0) {
            return CONST.NULL_COLLISION;
        }

        // Change referencial
        // Unit u is not at point (0, 0) with a speed vector of (0, 0)
        double x2 = x - u.x;
        double y2 = y - u.y;
        double r2 = radius + u.radius;
        double vx2 = vx - u.vx;
        double vy2 = vy - u.vy;

        // Resolving: sqrt((x + t*vx)^2 + (y + t*vy)^2) = radius <=> t^2*(vx^2 + vy^2) + t*2*(x*vx + y*vy) + x^2 + y^2 - radius^2 = 0
        // at^2 + bt + c = 0;
        // a = vx^2 + vy^2
        // b = 2*(x*vx + y*vy)
        // c = x^2 + y^2 - radius^2

        double a = vx2 * vx2 + vy2 * vy2;

        if (a <= 0.0) {
            return CONST.NULL_COLLISION;
        }

        double b = 2.0 * (x2 * vx2 + y2 * vy2);
        double c = x2 * x2 + y2 * y2 - r2 * r2;
        double delta = b * b - 4.0 * a * c;

        if (delta < 0.0) {
            return CONST.NULL_COLLISION;
        }

        double t = (-b - Math.sqrt(delta)) / (2.0 * a);

        if (t <= 0.0) {
            return CONST.NULL_COLLISION;
        }

        return new Collision(t, this, u);
    }

    // Bounce between 2 units
    void bounce(Unit u) {
        double mcoeff = (mass + u.mass) / (mass * u.mass);
        double nx = x - u.x;
        double ny = y - u.y;
        double nxnysquare = nx * nx + ny * ny;
        double dvx = vx - u.vx;
        double dvy = vy - u.vy;
        double product = (nx * dvx + ny * dvy) / (nxnysquare * mcoeff);
        double fx = nx * product;
        double fy = ny * product;
        double m1c = 1.0 / mass;
        double m2c = 1.0 / u.mass;

        vx -= fx * m1c;
        vy -= fy * m1c;
        u.vx += fx * m2c;
        u.vy += fy * m2c;

        fx = fx * CONST.IMPULSE_COEFF;
        fy = fy * CONST.IMPULSE_COEFF;

        // Normalize vector at min or max impulse
        double impulse = Math.sqrt(fx * fx + fy * fy);
        double coeff = 1.0;
        if (impulse > CONST.EPSILON && impulse < CONST.MIN_IMPULSE) {
            coeff = CONST.MIN_IMPULSE / impulse;
        }

        fx = fx * coeff;
        fy = fy * coeff;

        vx -= fx * m1c;
        vy -= fy * m1c;
        u.vx += fx * m2c;
        u.vy += fy * m2c;

        double diff = (distance(u) - radius - u.radius) / 2.0;
        if (diff <= 0.0) {
            // Unit overlapping. Fix positions.
            moveTo(u, diff - CONST.EPSILON);
            u.moveTo(this, diff - CONST.EPSILON);
        }
    }

    // Bounce with the map border
    void bounce() {
        double mcoeff = 1.0 / mass;
        double nxnysquare = x * x + y * y;
        double product = (x * vx + y * vy) / (nxnysquare * mcoeff);
        double fx = x * product;
        double fy = y * product;

        vx -= fx * mcoeff;
        vy -= fy * mcoeff;

        fx = fx * CONST.IMPULSE_COEFF;
        fy = fy * CONST.IMPULSE_COEFF;

        // Normalize vector at min or max impulse
        double impulse = Math.sqrt(fx * fx + fy * fy);
        double coeff = 1.0;
        if (impulse > CONST.EPSILON && impulse < CONST.MIN_IMPULSE) {
            coeff = CONST.MIN_IMPULSE / impulse;
        }

        fx = fx * coeff;
        fy = fy * coeff;
        vx -= fx * mcoeff;
        vy -= fy * mcoeff;

        double diff = distance(CONST.CENTER) + radius - CONST.MAP_RADIUS;
        if (diff >= 0.0) {
            // Unit still outside of the map, reposition it
            moveTo(CONST.CENTER, diff + CONST.EPSILON);
        }
    }

    @Override
    protected Unit clone() {
        return (Unit) super.clone();
    }
}

/**
 * tanker
 */
class Tanker extends Unit {
    int water;
    //int size;
    InnerPlayer innerPlayer;
    boolean killed;

    Tanker(int unitId, int x, int y, InnerPlayer innerPlayer, double radius) {
        super(CONST.TYPE_TANKER, unitId, x, y, radius);

        water = CONST.TANKER_EMPTY_WATER;
        mass = CONST.TANKER_EMPTY_MASS + CONST.TANKER_MASS_BY_WATER * water;
        friction = CONST.TANKER_FRICTION;
        //radius = CONST.TANKER_RADIUS_BASE + CONST.TANKER_RADIUS_BY_SIZE * size;
    }

    /*Wreck die() {
        // Don't spawn a wreck if our center is outside of the map
        if (distance(CONST.CENTER) >= CONST.MAP_RADIUS) {
            return null;
        }

        return new Wreck(Util.round(x), Util.round(y), water, radius);
    }

    boolean isFull() {
        return water >= size;
    }

    void play() {
        if (isFull()) {
            // Try to leave the map
            thrust(CONST.CENTER, -CONST.TANKER_THRUST);
        } else if (distance(CONST.CENTER) > CONST.WATERTOWN_RADIUS) {
            // Try to reach watertown
            thrust(CONST.CENTER, CONST.TANKER_THRUST);
        }
    }*/

    Collision getCollision() {
        // Tankers can go outside of the map
        return CONST.NULL_COLLISION;
    }

    /*public int getExtraInput() {
        return water;
    }

    public int getExtraInput2() {
        return size;
    }*/
    @Override
    protected Tanker clone() {
        return (Tanker) super.clone();
    }
}

class SkillResult {
    static final int OK = 0;
    static final int NO_RAGE = 1;
    static final int TOO_FAR = 2;
    Point target;
    int code;

    SkillResult(int x, int y) {
        target = new Point(x, y);
        code = OK;
    }

    int getX() {
        return (int) target.x;
    }

    int getY() {
        return (int) target.y;
    }
}

class InnerPlayer {
    int score;
    int index;
    int rage;
    Looter[] looters;
    boolean dead;
    Queue<TankerSpawn> tankers;

    InnerPlayer(int index) {
        this.index = index;

        looters = new Looter[CONST.LOOTER_COUNT];
    }

    void kill() {
        dead = true;
    }

    Reaper getReaper() {
        return (Reaper) looters[CONST.LOOTER_REAPER];
    }

    Destroyer getDestroyer() {
        return (Destroyer) looters[CONST.LOOTER_DESTROYER];
    }

    Doof getDoof() {
        return (Doof) looters[CONST.LOOTER_DOOF];
    }
}

abstract class Looter extends Unit {
    int skillCost;
    double skillRange;
    boolean skillActive;

    InnerPlayer innerPlayer;

    Point wantedThrustTarget;
    int wantedThrustPower;

    String message;
    Action attempt;
    SkillResult skillResult;

    Looter(int type, int unitId, InnerPlayer innerPlayer, double x, double y, double radius) {
        super(type, unitId, x, y, radius);

        this.innerPlayer = innerPlayer;

        radius = CONST.LOOTER_RADIUS;
    }

    SkillEffect skill(Point p) throws TooFarException, NoRageException {
        if (innerPlayer.rage < skillCost)
            throw new NoRageException();
        if (distance(p) > skillRange)
            throw new TooFarException();

        innerPlayer.rage -= skillCost;
        return skillImpl(p);
    }

    public int getPlayerIndex() {
        return innerPlayer.index;
    }

    abstract SkillEffect skillImpl(Point p);

    public void setWantedThrust(Point target, Integer power) {
        if (power < 0) {
            power = 0;
        }

        wantedThrustTarget = target;
        wantedThrustPower = Math.min(power, CONST.MAX_THRUST);
    }

    public void reset() {
        message = null;
        attempt = null;
        skillResult = null;
        wantedThrustTarget = null;
    }
}

class Reaper extends Looter {
    Reaper(int unitId, InnerPlayer innerPlayer, double x, double y, double radius) {
        super(unitId, CONST.LOOTER_REAPER, innerPlayer, x, y, radius);

        mass = CONST.REAPER_MASS;
        friction = CONST.REAPER_FRICTION;
        skillCost = CONST.REAPER_SKILL_COST;
        skillRange = CONST.REAPER_SKILL_RANGE;
        skillActive = CONST.REAPER_SKILL_ACTIVE;
    }

    SkillEffect skillImpl(Point p) {
        return new ReaperSkillEffect(CONST.TYPE_REAPER_SKILL_EFFECT, p.x, p.y, CONST.REAPER_SKILL_RADIUS, CONST.REAPER_SKILL_DURATION, CONST.REAPER_SKILL_ORDER, this);
    }

    protected Reaper clone() {
        return (Reaper) super.clone();
    }
}

class Destroyer extends Looter {
    Destroyer(int unitId, InnerPlayer innerPlayer, double x, double y, double radius) {
        super(unitId, CONST.LOOTER_DESTROYER, innerPlayer, x, y, radius);

        mass = CONST.DESTROYER_MASS;
        friction = CONST.DESTROYER_FRICTION;
        skillCost = CONST.DESTROYER_SKILL_COST;
        skillRange = CONST.DESTROYER_SKILL_RANGE;
        skillActive = CONST.DESTROYER_SKILL_ACTIVE;
    }

    SkillEffect skillImpl(Point p) {
        return new DestroyerSkillEffect(CONST.TYPE_DESTROYER_SKILL_EFFECT,
                p.x,
                p.y,
                CONST.DESTROYER_SKILL_RADIUS,
                CONST.DESTROYER_SKILL_DURATION,
                CONST.DESTROYER_SKILL_ORDER,
                this);
    }

    @Override
    protected Destroyer clone() {
        return (Destroyer) super.clone();
    }
}

class Doof extends Looter {
    Doof(int unitId, InnerPlayer innerPlayer, double x, double y, double radius) {
        super(unitId, CONST.LOOTER_DOOF, innerPlayer, x, y, radius);

        mass = CONST.DOOF_MASS;
        friction = CONST.DOOF_FRICTION;
        skillCost = CONST.DOOF_SKILL_COST;
        skillRange = CONST.DOOF_SKILL_RANGE;
        skillActive = CONST.DOOF_SKILL_ACTIVE;
    }

    SkillEffect skillImpl(Point p) {
        return new DoofSkillEffect(CONST.TYPE_DOOF_SKILL_EFFECT, p.x, p.y, CONST.DOOF_SKILL_RADIUS, CONST.DOOF_SKILL_DURATION, CONST.DOOF_SKILL_ORDER, this);
    }

    // With flame effects! Yeah!
    int sing() {
        return (int) Math.floor(speed() * CONST.DOOF_RAGE_COEF);
    }

    @Override
    protected Doof clone() {
        return (Doof) super.clone();
    }
}

class TankerSpawn {
    int size;
    double angle;

    TankerSpawn(int size, double angle) {
        this.size = size;
        this.angle = angle;
    }
}

class Collision {
    double t;
    Unit a;
    Unit b;

    Collision(double t) {
        this(t, null, null);
    }

    Collision(double t, Unit a) {
        this(t, a, null);
    }

    Collision(double t, Unit a, Unit b) {
        this.t = t;
        this.a = a;
        this.b = b;
    }

    Tanker dead() {
        if (a instanceof Destroyer && b instanceof Tanker && b.mass < CONST.REAPER_SKILL_MASS_BONUS) {
            return (Tanker) b;
        }

        if (b instanceof Destroyer && a instanceof Tanker && a.mass < CONST.REAPER_SKILL_MASS_BONUS) {
            return (Tanker) a;
        }

        return null;
    }
}

abstract class SkillEffect extends Point {
    int id;
    int type;
    double radius;
    int duration;
    int order;
    boolean known;
    Looter looter;

    SkillEffect(int type, double x, double y, double radius, int duration, int order, Looter looter) {
        super(x, y);

        // unitId = GLOBAL_ID++;

        this.type = type;
        this.radius = radius;
        this.duration = duration;
        this.looter = looter;
        this.order = order;
    }

    void apply(List<Unit> units) {
        duration -= 1;
        applyImpl(units.stream().filter(u -> isInRange(u, radius + u.radius)).collect(Collectors.toList()));
    }

    abstract void applyImpl(List<Unit> units);

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        SkillEffect other = (SkillEffect) obj;
        if (id != other.id) return false;
        return true;
    }
}

class ReaperSkillEffect extends SkillEffect {

    ReaperSkillEffect(int type, double x, double y, double radius, int duration, int order, Reaper reaper) {
        super(type, x, y, radius, duration, order, reaper);
    }

    void applyImpl(List<Unit> units) {
        // Increase mass
        units.forEach(u -> u.mass += CONST.REAPER_SKILL_MASS_BONUS);
    }
}

class DestroyerSkillEffect extends SkillEffect {

    DestroyerSkillEffect(int type, double x, double y, double radius, int duration, int order, Destroyer destroyer) {
        super(type, x, y, radius, duration, order, destroyer);
    }

    void applyImpl(List<Unit> units) {
        // Push units
        units.forEach(u -> u.thrust(this, -CONST.DESTROYER_NITRO_GRENADE_POWER));
    }
}

class DoofSkillEffect extends SkillEffect {

    DoofSkillEffect(int type, double x, double y, double radius, int duration, int order, Doof doof) {
        super(type, x, y, radius, duration, order, doof);
    }

    void applyImpl(List<Unit> units) {
        // Nothing to do now
    }
}

@SuppressWarnings("serial")
class NoRageException extends Exception {
}

@SuppressWarnings("serial")
class TooFarException extends Exception {
}

enum Action {
    SKILL, MOVE, WAIT;
}

class Util {
    static int round(double x) {
        int s = x < 0 ? -1 : 1;
        return s * (int) Math.round(s * x);
    }

    static double getDistance(Point a, Point b) {
        double diffx = a.x - b.x;
        double diffy = a.y - b.y;
        return Math.sqrt(diffx * diffx + diffy * diffy);
    }

    static int getRanged(int val, int min, int max) {
        return Math.min(Math.max(min, val), max);
    }

    static double getRanged(double val, int min, int max) {
        return getRanged((int) val, min, max);
    }

    static String getOutput(double x, double y, double thrust) {
        return getOutput((int) x, (int) y, (int) thrust);
    }

    static String getOutput(int x, int y, int thrust) {
        return String.format("%s %s %s", x, y, thrust);
    }

}

class AIParam {
    static int SCORE = 100000;
    static int RDist2Wreck = 1000;
    //static int POpDist2Wreck
    static int RDist2Tanker = 100;
    static int SEARCH_MSEC = 50;
}

class CONST {

    static boolean SPAWN_WRECK = false;
    static int LOOTER_COUNT = 3;
    static boolean REAPER_SKILL_ACTIVE = true;
    static boolean DESTROYER_SKILL_ACTIVE = true;
    static boolean DOOF_SKILL_ACTIVE = true;

    static double MAP_RADIUS = 6000.0;

    static double WATERTOWN_RADIUS = 3000.0;

    static int TANKER_THRUST = 500;
    static double TANKER_EMPTY_MASS = 2.5;
    static double TANKER_MASS_BY_WATER = 0.5;
    static double TANKER_FRICTION = 0.40;
    static int TANKER_EMPTY_WATER = 1;
    static double TANKER_SPAWN_RADIUS = 8000.0;
    static int TANKER_START_THRUST = 2000;

    static int MAX_THRUST = 300;
    static int MAX_RAGE = 300;
    static int WIN_SCORE = 50;

    static double REAPER_MASS = 0.5;
    static double REAPER_FRICTION = 0.20;
    static int REAPER_SKILL_DURATION = 3;
    static int REAPER_SKILL_COST = 30;
    static int REAPER_SKILL_ORDER = 0;
    static double REAPER_SKILL_RANGE = 2000.0;
    static double REAPER_SKILL_RADIUS = 1000.0;
    static double REAPER_SKILL_MASS_BONUS = 10.0;

    static double DESTROYER_MASS = 1.5;
    static double DESTROYER_FRICTION = 0.30;
    static int DESTROYER_SKILL_DURATION = 1;
    static int DESTROYER_SKILL_COST = 60;
    static int DESTROYER_SKILL_ORDER = 2;
    static double DESTROYER_SKILL_RANGE = 2000.0;
    static double DESTROYER_SKILL_RADIUS = 1000.0;
    static int DESTROYER_NITRO_GRENADE_POWER = 1000;

    static double DOOF_MASS = 1.0;
    static double DOOF_FRICTION = 0.25;
    static double DOOF_RAGE_COEF = 1.0 / 100.0;
    static int DOOF_SKILL_DURATION = 3;
    static int DOOF_SKILL_COST = 30;
    static int DOOF_SKILL_ORDER = 1;
    static double DOOF_SKILL_RANGE = 2000.0;
    static double DOOF_SKILL_RADIUS = 1000.0;

    static double LOOTER_RADIUS = 400.0;
    static int LOOTER_REAPER = 0;
    static int LOOTER_DESTROYER = 1;
    static int LOOTER_DOOF = 2;

    static int TYPE_TANKER = 3;
    static int TYPE_WRECK = 4;
    static int TYPE_REAPER_SKILL_EFFECT = 5;
    static int TYPE_DOOF_SKILL_EFFECT = 6;
    static int TYPE_DESTROYER_SKILL_EFFECT = 7;

    static double EPSILON = 0.00001;
    static double MIN_IMPULSE = 30.0;
    static double IMPULSE_COEFF = 0.5;

    // Center of the map
    static final Point CENTER = new Point(0, 0);

    // The null collision
    static final Collision NULL_COLLISION = new Collision(1.0 + EPSILON);

    static int MOVE_R = 600;
    static Integer[] target_X;
    static Integer[] target_Y;
    static int DIR_NUM = 12;

    static Integer[] ACCE = {50, 100, 200, 300};

    static {
        int diffDeg = 360 / DIR_NUM;
        List<Integer> x = new ArrayList<>();
        List<Integer> y = new ArrayList<>();
        for (int i = 0; i < 360; i += diffDeg) {
            x.add((int)Math.round(MOVE_R * Math.cos(Math.toRadians(i))));
            y.add((int)Math.round(MOVE_R * Math.sin(Math.toRadians(i))));
        }
        target_X = x.toArray(new Integer[0]);
        target_Y = y.toArray(new Integer[0]);
    }


}

class SimpleEvaluator implements Evaluator {
    @Override
    public double eval(State s) {
        final double[] rDist2Wrecks={0}; //自分のreaperがWreckに近い報酬
        final double[] rDist2Tankers={0}; //自分のreaperがTankerに近い報酬

        //final double[] pDistToWrecks={0}; //相手のreaperがWreckに近い報酬
        //final double[] pDistToTankers={0}; //相手のreaperがTankerに近い報酬

        s.wrecks.forEach(w -> {
            rDist2Wrecks[0] += (Math.pow(AIParam.RDist2Wreck,
                    (CONST.MAP_RADIUS - Util.getDistance(s.myReaper, w))/CONST.MAP_RADIUS))* w.water;
            //pDistToWrecks[0] +=
        });

        s.tankers.forEach(t -> {
            rDist2Tankers[0] += (Math.pow(AIParam.RDist2Tanker,
                    (CONST.MAP_RADIUS = Util.getDistance(s.myDestroyer, t))/CONST.MAP_RADIUS)) * t.water;
        });

        int opScore_higher = s.enemyScore1 > s.enemyScore2 ?
                                s.enemyScore1 : s.enemyScore2;
        int rpScore = (s.myScore - opScore_higher) * AIParam.SCORE;
        return rpScore + rDist2Wrecks[0] + rDist2Tankers[0];
    }
}

interface Evaluator {
    double eval(State s);
}