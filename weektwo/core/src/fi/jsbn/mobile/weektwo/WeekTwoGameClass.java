package fi.jsbn.mobile.weektwo;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.TimeUtils;

import java.sql.Time;
import java.util.ArrayList;

class GameObject {
    float x, y;
}

class Bullet {
    Rectangle rect;
    Texture text;
    boolean active;
    float speed = 1.0f;
    Bullet(Rectangle rect, Texture text, float speed) {
        this.rect = rect;
        this.text = text;
        this.speed = speed;
        this.active = true;
    }
    void move() {
        this.rect.x += this.speed;
    }
}

class DiagBullet extends Bullet {
    float xSpeed;
    float ySpeed;
    DiagBullet(Rectangle rect, Texture text, float xSpeed, float ySpeed) {
        super(rect, text, 0.0f);
        this.xSpeed = xSpeed;
        this.ySpeed = ySpeed;
    }

    @Override
    public void move() {
        this.rect.x += this.xSpeed;
        this.rect.y += this.ySpeed;
    }
}


class Scenery extends GameObject {
    float scale;
    float speed;
    int layer;
    Texture texture;
    Rectangle sceneryRect;
    static public Scenery getRandomBackgroundScenery(float x, float y) {
        int randomNumber = (int) (Math.random() * 3);
        switch (randomNumber) {
            default:
                return new Mountain(x, y);
        }
    }
    static public Scenery getRandomForegroundScenery(float x, float y) {
        int randomNumber = 0;
        switch (randomNumber) {
            default:
                return new House(x, y);
        }
    }

    Scenery(Texture texture, float startPosX, float startPosY, float scale, float speed, int layer) {
        this.texture = texture;
        this.speed = speed;
        this.scale = scale;
        this.layer = layer;
        this.sceneryRect = new Rectangle(startPosX, startPosY, this.texture.getWidth() / scale, this.texture.getHeight() / scale);
    }
}

class Mountain extends Scenery {
    Mountain(float startPosX, float startPosY) {
        super(new Texture(Gdx.files.internal("mtns.png")), startPosX, startPosY, 10.0f, -0.01f, 0);
    }
}

class House extends Scenery {
    House(float startPosX, float startPosY) {
        super(new Texture(Gdx.files.internal("house1.png")), startPosX, startPosY, 25.0f + ((float) Math.random() * 9), -0.08f, 1);
    }
}


class Ship extends GameObject {

    public int health;
    public long lastDamagedTime;
    public long shipDeadTime;

    public void getHit() {
        this.health--;
        this.lastDamagedTime = TimeUtils.millis();
    }

    enum ShipState {STATE_NEUTRAL, STATE_MOVING_UP, STATE_MOVING_DOWN};

    ArrayList<Bullet> bullets = new ArrayList<Bullet>();
    ShipState shipState;
    Rectangle shipRect;
    Sound shotSound = Gdx.audio.newSound(Gdx.files.internal("pew.wav"));
    Texture shipTexture;
    Texture bulletTexture = new Texture(Gdx.files.internal("bullet1.png"));
    Texture baseTexture = new Texture(Gdx.files.internal("ship1.png"));
    Texture movingUpTexture = new Texture(Gdx.files.internal("ship2.png"));
    Texture movingDownTexture = new Texture(Gdx.files.internal("ship3.png"));
    Texture deadTexture = new Texture(Gdx.files.internal("deadenemy.png"));


    long lastStateChange;
    long lastFiredTime;
    long fireCooldown;

    Ship(float x, float y) {
        shipTexture = baseTexture;
        lastDamagedTime = 0;
        shipRect = new Rectangle(x, y, shipTexture.getWidth() / 20.0f, shipTexture.getHeight() / 20.0f);
        lastFiredTime = 0L;
        fireCooldown = 100L;
        health = 3;
        shipDeadTime = 0;
    }

    public boolean shouldChangeState() {
        return (TimeUtils.timeSinceMillis(lastStateChange) > 50L);
    }

    public void fire() {
        if (TimeUtils.timeSinceMillis(lastFiredTime) > fireCooldown) {
            float bulletWidth = bulletTexture.getWidth() / 20.0f;
            float bulletHeight = bulletTexture.getHeight() / 20.0f;
            bullets.add(new Bullet(new Rectangle(this.x + this.shipRect.getWidth() * 1.6f, this.shipRect.getY() + bulletHeight * 1.5f, bulletWidth, bulletHeight), bulletTexture, 1.0f));
            shotSound.play();
            lastFiredTime = TimeUtils.millis();
        }
    }

    public Texture getTexture() {
        if (this.health <= 0) {
            return deadTexture;
        }
        switch (shipState) {
            case STATE_NEUTRAL:
                return baseTexture;
            case STATE_MOVING_UP:
                return movingUpTexture;
            case STATE_MOVING_DOWN:
                return movingDownTexture;
            default:
                return baseTexture;
        }
    }

    public void setShipState(ShipState state) {
        this.shipState = state;
        this.lastStateChange = TimeUtils.millis();
    }

    public ShipState getShipState(ShipState state) {
        return this.shipState;
    }

    public void setY(float y) {
            this.shipRect.setY(y);
    }
    public float getY() {
        return this.shipRect.getY();
    }
}

abstract class Enemy extends GameObject {

    Texture enemyTexture;
    Texture deadTexture;
    Rectangle enemyRect;
    boolean alive;
    boolean dying;
    float scale;
    float fireDelay;
    float speed;
    int health;
    long takeDamageDelay;
    long lastTakenDamage;
    long lastFireTime;
    long deathTime;
    ArrayList<Bullet> enemyBullets = new ArrayList<Bullet>();
    ArrayList<Bullet> bulletsToRemove = new ArrayList<Bullet>();
    Sound deathSound = Gdx.audio.newSound(Gdx.files.internal("boom.wav"));

    abstract void move();
    abstract void fire();
    static Enemy getRandomEnemy(float x, float y) {
        int randomNumber = (int) (Math.random() * 2);
        switch (randomNumber) {
            case 0:
                return new ShootyBoy(x, y);
            case 1:
                return new DoubleShootyBoy(x, y);
            default:
                return new ShootyBoy(x, y);
        }
    }

    void takeDamage() {
        this.health -= 1;
        if (this.health <= 0 && !this.dying && this.alive) {
            this.die();
        }
        this.lastTakenDamage = TimeUtils.millis();
    }

    void die() {
        this.dying = true;
        this.enemyTexture = this.deadTexture;

        this.enemyRect.width = this.enemyTexture.getWidth() / 100.0f;
        this.enemyRect.height = this.enemyTexture.getHeight() / 100.0f;
        this.deathTime = TimeUtils.millis();
        if (this.alive)
            this.deathSound.play();
    }

    Enemy() {
        this.lastTakenDamage = 0;
        this.alive = true;
    }

    void dispose() {
    }
}

class FastBoy extends Enemy {
    float scale = 15f;
    FastBoy(float x, float y) {
        super();
        this.enemyTexture = new Texture(Gdx.files.internal("enemy1.png"));
        this.deadTexture = new Texture(Gdx.files.internal("deadenemy.png"));
        this.enemyRect = new Rectangle(x, y, enemyTexture.getWidth() / scale, enemyTexture.getHeight() / scale);
        this.speed = -0.15f;
    }

    public void fire() {}
    void move() {
        this.enemyRect.x += this.speed;
    }
}

class ShootyBoy extends Enemy {
    Texture bulletTexture;
    float bulletWidth;
    float bulletHeight;
    float scale = 10.0f;

    ShootyBoy(float x, float y) {
        super();
        this.enemyTexture = WeekTwoGameClass.shootyBoyTexture;
        this.deadTexture = WeekTwoGameClass.deadShootyBoyTexture;
        this.enemyRect = new Rectangle(x, y, enemyTexture.getWidth() / scale, enemyTexture.getHeight() / scale);
        this.speed = -0.08f;
        bulletTexture = new Texture(Gdx.files.internal("bullet2.png"));
        float bulletWidth = bulletTexture.getWidth() / 20.0f;
        float bulletHeight = bulletTexture.getHeight() / 20.0f;
        fireDelay = 800L / WeekTwoGameClass.difficultyMultiplier;
        enemyBullets = new ArrayList<Bullet>();
    }

    public void move() {
        this.enemyRect.x += this.speed;
    }
    public void fire() {
       this.enemyBullets.add(new Bullet(new Rectangle(this.enemyRect.x, this.enemyRect.y + (this.enemyRect.getHeight() / 2), 0.5f, 0.5f), bulletTexture, -0.2f));
        //this.enemyBullets.add(new Bullet(new Rectangle(this.enemyRect.x, this.enemyRect.y, bulletWidth, bulletHeight), bulletTexture, -0.3f));
        this.lastFireTime = TimeUtils.millis();
    }
}

class DoubleShootyBoy extends Enemy {
    Texture bulletTexture;
    float bulletWidth;
    float bulletHeight;
    float scale = 10.0f;

    DoubleShootyBoy(float x, float y) {
        super();
        this.enemyTexture = WeekTwoGameClass.doubleShootyBoyTexture;
        this.deadTexture = WeekTwoGameClass.deadDoubleShootyBoyTexture;
        this.enemyRect = new Rectangle(x, y, enemyTexture.getWidth() / scale, enemyTexture.getHeight() / scale);
        this.speed = -0.08f;
        bulletTexture = new Texture(Gdx.files.internal("bullet2.png"));
        float bulletWidth = bulletTexture.getWidth() / 20.0f;
        float bulletHeight = bulletTexture.getHeight() / 20.0f;
        fireDelay = 1000L / WeekTwoGameClass.difficultyMultiplier;
        enemyBullets = new ArrayList<Bullet>();
    }

    public void move() {
        this.enemyRect.x += this.speed;
    }
    public void fire() {
        this.enemyBullets.add(new DiagBullet(new Rectangle(this.enemyRect.x, this.enemyRect.y + (this.enemyRect.getHeight() / 2), 0.5f, 0.5f), bulletTexture, -0.2f, -0.02f));
        this.enemyBullets.add(new DiagBullet(new Rectangle(this.enemyRect.x, this.enemyRect.y + (this.enemyRect.getHeight() / 2), 0.5f, 0.5f), bulletTexture, -0.2f, 0.02f));
        this.lastFireTime = TimeUtils.millis();
    }
}

public class WeekTwoGameClass extends ApplicationAdapter {

    static Texture shootyBoyTexture;
    static Texture doubleShootyBoyTexture;
    static Texture deadShootyBoyTexture;
    static Texture deadDoubleShootyBoyTexture;
    static Texture shootyBoyBulletTexture;
    static Texture doubleShootyBoyBulletTexture;
    static Texture houseTexture;
    static Texture mountainTexture;

    enum GameState {GAME_STATE_START, GAME_STATE_RUNNING, GAME_STATE_OVER};

    Music bgMusic;
    ArrayList<Scenery> scenery;
    ArrayList<Enemy> enemies;
    GameState gameState;
    int score;
    SpriteBatch batch;
    OrthographicCamera camera;
    Ship ship;
    Texture backGround;
    Texture ground;
    Texture introText;
    float startingAngle;
    long foregroundSpawnDelay = 300L;
	long lastForegroundSpawn = 0L;
    long enemySpawnDelay = 2000L;
	long lastEnemySpawn;
	static float difficultyMultiplier = 1.00f;
    BitmapFont font;
    Sound failSound;

    public void setGameState(GameState gameState) {
        switch(gameState) {
            case GAME_STATE_START:
                this.resetGame();
                camera.setToOrtho(false);
                batch.setProjectionMatrix(camera.combined);
                this.gameState = GameState.GAME_STATE_START;
                break;
            case GAME_STATE_RUNNING:
                this.resetGame();
                bgMusic.play();
                camera.setToOrtho(false, 16, 9);
                batch.setProjectionMatrix(camera.combined);
                ship.shipRect.y = camera.viewportHeight / 2;
                this.gameState = GameState.GAME_STATE_RUNNING;
                Gdx.gl.glClearColor(1, 1, 1, 1);
                break;
            case GAME_STATE_OVER:
                camera.setToOrtho(false);
                batch.setProjectionMatrix(camera.combined);
                bgMusic.stop();
                failSound.play();
                this.gameState = GameState.GAME_STATE_OVER;
                break;
        }
    }

    public void resetGame() {
        score = 0;
        ship.health = 3;
        lastEnemySpawn = TimeUtils.millis() + 2000L;
        ship.bullets.clear();
        for (Enemy e : enemies) {
            e.dispose();
        }
        enemies.clear();
        lastEnemySpawn = TimeUtils.millis() + 2000L;
    }

	@Override
	public void create () {

        shootyBoyTexture = new Texture(Gdx.files.internal("enemy2.png"));
        doubleShootyBoyTexture = new Texture(Gdx.files.internal("enemy3.png"));
        deadDoubleShootyBoyTexture = new Texture(Gdx.files.internal("deadenemy.png"));
        deadShootyBoyTexture = new Texture(Gdx.files.internal("deadenemy.png"));
        shootyBoyBulletTexture = new Texture(Gdx.files.internal("bullet2.png"));
        doubleShootyBoyBulletTexture = new Texture(Gdx.files.internal("bullet2.png"));

        failSound = Gdx.audio.newSound(Gdx.files.internal("failure.wav"));
        bgMusic = Gdx.audio.newMusic(Gdx.files.internal("ropocalypse.mp3"));
		camera = new OrthographicCamera();
		batch = new SpriteBatch();
		ship = new Ship(1, camera.viewportHeight / 2);
		scenery = new ArrayList<Scenery>();
        enemies = new ArrayList<Enemy>();
		scenery.add(Scenery.getRandomBackgroundScenery(camera.viewportWidth, 0));
        font = new BitmapFont();
        font.setColor(Color.BLACK);
        backGround = new Texture(Gdx.files.internal("bg.png"));
        ground = new Texture(Gdx.files.internal("ground.png"));
		introText = new Texture(Gdx.files.internal("intro.png"));
        setGameState(GameState.GAME_STATE_START);
    }

	@Override
	public void render () {
        Gdx.gl.glClearColor(1, 1, 0.9f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

	    if (gameState == GameState.GAME_STATE_START) {
	        camera.setToOrtho(false);
	        batch.setProjectionMatrix(camera.combined);
	        batch.begin();
            batch.draw(introText, (Gdx.graphics.getWidth() - introText.getWidth()) / 2, (Gdx.graphics.getHeight() - introText.getHeight()) / 2);
            batch.end();

            if (Gdx.input.isTouched()) {
                startingAngle = Gdx.input.getRoll();
                setGameState(GameState.GAME_STATE_RUNNING);
            }

        } else if (gameState == GameState.GAME_STATE_RUNNING) {
            if (ship.health > 0) {
                if (((startingAngle - Gdx.input.getRoll() > 15.0f) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) && ship.getY() > 0) {
                    if (ship.shouldChangeState())
                        ship.setShipState(Ship.ShipState.STATE_MOVING_DOWN);
                    ship.setY(ship.getY() - 0.1f);
                } else if (((startingAngle - Gdx.input.getRoll() < -15.0f) || Gdx.input.isKeyPressed(Input.Keys.UP)) && ship.getY() < camera.viewportHeight - 1) {
                    if (ship.shouldChangeState())
                        ship.setShipState(Ship.ShipState.STATE_MOVING_UP);
                    ship.setY(ship.getY() + 0.1f);
                } else {
                    if (ship.shouldChangeState())
                        ship.setShipState(Ship.ShipState.STATE_NEUTRAL);
                }
                if (Gdx.input.isTouched()) {
                    ship.fire();
                }
            } else {
                if (TimeUtils.timeSinceMillis(ship.shipDeadTime) > 500L) {
                    setGameState(GameState.GAME_STATE_OVER);
                }
            }

            if (TimeUtils.timeSinceMillis(lastForegroundSpawn) > foregroundSpawnDelay) {
                scenery.add(Scenery.getRandomForegroundScenery(camera.viewportWidth, (float) (Math.random() * 0.7)));
                lastForegroundSpawn = TimeUtils.millis();
            }

            if (TimeUtils.timeSinceMillis(lastEnemySpawn) > enemySpawnDelay) {
                enemies.add(Enemy.getRandomEnemy(camera.viewportWidth, (float) (Math.random() * (camera.viewportHeight * 0.8))));
                lastEnemySpawn = TimeUtils.millis();
            }

            batch.begin();
            batch.draw(backGround, 0, 0, backGround.getWidth() / 10.0f, backGround.getHeight() / 10.0f);
            for (Scenery s : scenery) {
                if (s.layer == 0) {
                    batch.draw(s.texture, s.sceneryRect.x, 0.0f, s.sceneryRect.getWidth(), s.sceneryRect.getHeight());
                    s.sceneryRect.x += s.speed;
                }
            }
            batch.draw(ground, 0, 0, ground.getWidth() / 10.0f, ground.getHeight() / 10.0f);
            for (Scenery s : scenery) {
                if (s.layer == 1) {
                    batch.draw(s.texture, s.sceneryRect.x, s.sceneryRect.y, s.sceneryRect.getWidth(), s.sceneryRect.getHeight());
                    s.sceneryRect.x += s.speed;
                }
            }
            if (ship.health >= 0)
                batch.draw(ship.getTexture(), 1, ship.getY(), ship.shipRect.width, ship.shipRect.height);

            for (Enemy e : enemies) {
                if (e.alive)
                    batch.draw(e.enemyTexture, e.enemyRect.x, e.enemyRect.y, e.enemyRect.getWidth(), e.enemyRect.getHeight());
                e.move();
                if (e.fireDelay != 0 && TimeUtils.timeSinceMillis(e.lastFireTime) > e.fireDelay && e.alive && !e.dying) {
                    Gdx.app.log("f", "fire");
                    e.fire();
                }
                for (Bullet b : e.enemyBullets) {
                    if (b.rect.x <= 0) {
                        b.active = false;
                    }
                    if (b.active)
                        batch.draw(b.text, b.rect.getX(), b.rect.getY(), b.rect.getWidth(), b.rect.getHeight());
                    else
                        e.bulletsToRemove.add(b);
                    b.move();
                    if (b.rect.overlaps(ship.shipRect) && b.active && TimeUtils.timeSinceMillis(ship.lastDamagedTime) > 100L) {
                        ship.health -= 1;
                        Gdx.app.log("dakmg", "damg");
                        ship.lastDamagedTime = TimeUtils.millis();
                        if (ship.health <= 0) {
                            ship.shipTexture = ship.deadTexture;
                            ship.shipDeadTime = TimeUtils.millis();
                        }
                        b.active = false;
                    }
                }
                e.enemyBullets.removeAll(e.bulletsToRemove);
                e.bulletsToRemove.clear();
                if (e.dying) {
                    if (TimeUtils.timeSinceMillis(e.deathTime) > 200L) {
                        e.alive = false;
                        e.dying = false;
                        setScore(getScore() + 1);
                    }
                }
                if (e.alive && !e.dying && TimeUtils.timeSinceMillis(e.lastTakenDamage) > 50L) {
                    for (Bullet b : ship.bullets) {
                        if (b.rect.overlaps(e.enemyRect) && b.active) {
                            e.takeDamage();
                            if (e.health <= 0) {
                                difficultyMultiplier *= 1.04f;
                                difficultyMultiplier = MathUtils.clamp(difficultyMultiplier, 1.0f, 2.0f);
                                setDifficulty();
                                Gdx.app.log("Debug", "FG: " + foregroundSpawnDelay + "ESD: " + enemySpawnDelay);
                            }
                            b.active = false;
                        }
                    }
                }
            }
            for (Bullet b : ship.bullets) {
                if (b.active)
                    batch.draw(b.text, b.rect.getX(), b.rect.getY(), b.rect.getWidth(), b.rect.getHeight());
                b.move();
            }
            batch.end();
        } else {
	        batch.begin();
	        font.getData().setScale(5.0f);
            font.draw(batch, "game over!!!\nScore: " + score + "\n\nTap to restart", Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
            batch.end();
            if (Gdx.input.isTouched()) {
                setGameState(GameState.GAME_STATE_RUNNING);
            }
        }
	}

    private int getScore() {
        return score;
    }

    public void setScore(int score) {
	    this.score = score;
    }
	
	@Override
	public void dispose () {
		batch.dispose();
	}

    public static float getDifficulty() {
        return difficultyMultiplier;
    }

    private void setDifficulty() {
        enemySpawnDelay = 2000L / (long) difficultyMultiplier;
    }


}
