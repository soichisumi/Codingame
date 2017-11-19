import javax.sound.sampled.Port;
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
                        if(player==0){
                            myReaper = r;
                        }else if(player ==-1){

                        }else{
                            opReapers.add(r);
                        }
                        break;
                    case 1: //Destroyer
                        Destroyer d = new Destroyer(unitId, null, x, y, radius);
                        if(player==0){
                            myDestroyer = d;
                        }else if(player == -1){

                        }else{
                            opDestroyers.add(d);
                        }
                        break;
                    case 3: //Tanker
                        Tanker t = new Tanker( unitId, x, y, null, radius);
                        tankers.add(t);
                        break;
                    case 4: //wreck
                        Wreck w = new Wreck(x, y, water, radius);
                        wrecks.add(w);
                        break;
                }
            }
            System.err.println(String.format("nW, nT: %s %s",wrecks.size(), tankers.size() ) );
            String moveR = moveReaper(myReaper, wrecks, tankers);
            String moveD = moveDestroyer(myDestroyer, myReaper, tankers);
            System.out.println(moveR + " "+moveR);
            System.out.println(moveD + " "+moveD);
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

    static <T extends Point> T getInFieldClosest(Reaper r, List<T> points, double rangeFromCenter){
        List<T> res = new ArrayList<>();
        for(T p : points){
            if(Util.getDistance(p, CONST.WATERTOWN)< rangeFromCenter)
                res.add(p);
        }
        return getClosest(r, res);
    }


    static String moveReaper(Reaper r, List<Wreck> wrecks, List<Tanker> tankers) {
        Wreck w = getClosest(r, wrecks);
        Tanker t = getInFieldClosest(r, tankers, 6500);

        if(w==null) {
            if(t ==null){
                return "WAIT";
            }
            return Util.getOutput(t.x, t.y,
                    Util.getRanged(Util.getDistance(r, t) / 3500 * 300, 100, 300));
        }

        double dist = Util.getDistance(r, w);

        if(w.water > 1) {
            return Util.getOutput( w.x,  w.y,
                    Util.getRanged(dist / 3500 * 300, 100, 200));
        }

        /*if(w.water > 1 && dist < w.radius) {
            System.err.println(String.format("reaper x y: %s %s", (int)r.x, (int)r.y));
            System.err.println(String.format("wreck x y rad/ %s %s %s %s", (int)w.x, (int)w.y, w.radius, dist));
            return "WAIT";
        }*/
        System.err.println(String.format("target: %s %s",w.radius, w.water ) );
        return Util.getOutput( w.x,  w.y,
             Util.getRanged(dist / 3500 * 300, 100, 200));
    }

    static String moveDestroyer(Destroyer d, Reaper r, List<Tanker> tankers){

        Tanker t = getInFieldClosest(r, tankers, 7000);
        if(t==null) {
            double dist = Util.getDistance(d, r);
            return Util.getOutput( r.x,  r.y,300);
                    //Util.getRanged(dist/ 300 * 300, 50, 300));
        }
        double dist = Util.getDistance(r, t);
        return Util.getOutput( t.x,  t.y,300);
                     //Util.getRanged(dist / 2000 * 300, 100, 300));
    }

    static List<String> solve() {
        return null;
    }
}

/**
 * 状態
 */
class State {
    int myScore;
    int enemyScore1;
    int enemyScore2;
    int myRage;
    int enemyRage1;
    int enemyRage2;
    double stateScore;

    Reaper myReaper;
    List<Reaper> opReapers;
    List<Wreck> wrecks;
    final List<String> commands = null;

    public State(Reaper myReaper, List<Reaper> opReapers,
                 List<Wreck> wrecks) {
        this.myReaper = myReaper;
        this.opReapers = opReapers;
        this.wrecks = wrecks;
    }
}

/**
 * 位置
 */
class Point {
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
        if (distance(CONST.WATERTOWN) + radius >= CONST.MAP_RADIUS) {
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

        double diff = distance(CONST.WATERTOWN) + radius - CONST.MAP_RADIUS;
        if (diff >= 0.0) {
            // Unit still outside of the map, reposition it
            moveTo(CONST.WATERTOWN, diff + CONST.EPSILON);
        }
    }

    public int getExtraInput() {
        return -1;
    }

    public int getExtraInput2() {
        return -1;
    }

    public int getPlayerIndex() {
        return -1;
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

        this.innerPlayer = innerPlayer;

        water = CONST.TANKER_EMPTY_WATER;
        mass = CONST.TANKER_EMPTY_MASS + CONST.TANKER_MASS_BY_WATER * water;
        friction = CONST.TANKER_FRICTION;
        //radius = CONST.TANKER_RADIUS_BASE + CONST.TANKER_RADIUS_BY_SIZE * size;
    }

    /*Wreck die() {
        // Don't spawn a wreck if our center is outside of the map
        if (distance(CONST.WATERTOWN) >= CONST.MAP_RADIUS) {
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
            thrust(CONST.WATERTOWN, -CONST.TANKER_THRUST);
        } else if (distance(CONST.WATERTOWN) > CONST.WATERTOWN_RADIUS) {
            // Try to reach watertown
            thrust(CONST.WATERTOWN, CONST.TANKER_THRUST);
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
        return getRanged( (int)val, min, max);
    }

    static String getOutput(double x, double y, double thrust){
        return getOutput((int)x, (int)y, (int)thrust);
    }

    static String getOutput(int x, int y, int thrust){
        return String.format("%s %s %s", x, y, thrust);
    }
}

class CONST {
    static boolean SPAWN_WRECK = false;
    static int LOOTER_COUNT = 3;
    static boolean REAPER_SKILL_ACTIVE = true;
    static boolean DESTROYER_SKILL_ACTIVE = true;
    static boolean DOOF_SKILL_ACTIVE = true;

    static double MAP_RADIUS = 6000.0;
    static int TANKERS_BY_PLAYER;
    static int TANKERS_BY_PLAYER_MIN = 1;
    static int TANKERS_BY_PLAYER_MAX = 3;

    static double WATERTOWN_RADIUS = 3000.0;

    static int TANKER_THRUST = 500;
    static double TANKER_EMPTY_MASS = 2.5;
    static double TANKER_MASS_BY_WATER = 0.5;
    static double TANKER_FRICTION = 0.40;
    static double TANKER_RADIUS_BASE = 400.0;
    static double TANKER_RADIUS_BY_SIZE = 50.0;
    static int TANKER_EMPTY_WATER = 1;
    static int TANKER_MIN_SIZE = 4;
    static int TANKER_MAX_SIZE = 10;
    static double TANKER_MIN_RADIUS = TANKER_RADIUS_BASE + TANKER_RADIUS_BY_SIZE * TANKER_MIN_SIZE;
    static double TANKER_MAX_RADIUS = TANKER_RADIUS_BASE + TANKER_RADIUS_BY_SIZE * TANKER_MAX_SIZE;
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

    // Global first free unitId for all elements on the map
    static int GLOBAL_ID = 0;

    // Center of the map
    final static Point WATERTOWN = new Point(0, 0);

    // The null collision
    final static Collision NULL_COLLISION = new Collision(1.0 + EPSILON);
}