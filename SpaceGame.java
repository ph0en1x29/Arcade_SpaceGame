import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;

/** Project: Solo Lab 7 Assignment
 * Purpose Details: Spacegame
 * Course: IST242
 * Author: LI ZHE YOW
 * Date Developed: 6/21/2024
 * Last Date Changed: 6/21/2024
 * Rev:1

 */
public class SpaceGame extends JFrame implements KeyListener {
    // Game configuration constants
    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;
    private static final int PLAYER_WIDTH = 70;
    private static final int PLAYER_HEIGHT = 70;
    private static final int PROJECTILE_WIDTH = 5;
    private static final int PROJECTILE_HEIGHT = 10;
    private static final int PLAYER_SPEED = 25;
    private static final int PROJECTILE_SPEED = 20;
    private static final int NUMBER_OF_STARS = 200;
    private static final int SPRITE_WIDTH = 64;
    private static final int SPRITE_HEIGHT = 64;
    private static final int NUM_SPRITES = 4;
    private static final int POWERUP_SIZE = 30;
    private static final double POWERUP_SPAWN_CHANCE = 0.01;
    private static final int INITIAL_OBSTACLE_SPEED = 3;
    private static final int LEVEL_TIME = 30000; // 30 seconds per level

    // Game state variables
    private int score = 0;
    private int playerHealth = 1; // Player starts with 1 life
    private int level = 1;
    private double obstacleSpeed = INITIAL_OBSTACLE_SPEED;
    private long gameStartTime;
    private boolean levelUpDisplayed = false;

    // Game components
    private JPanel gamePanel;
    private JLabel scoreLabel, healthLabel, levelLabel, timerLabel;
    private Timer timer;
    private boolean isGameOver;
    private int playerX, playerY;
    private int projectileX, projectileY;
    private boolean isProjectileVisible;
    private boolean isFiring;
    private boolean shieldActive;
    private long shieldStartTime;
    private long shieldDuration = 3000; // Shield lasts for 3 seconds
    private List<Point> obstacles;
    private List<Point> powerUps;
    private Random random;
    private Image spaceshipImage;
    private BackgroundStars backgroundStars;
    private BufferedImage spriteSheet;
    private BufferedImage[] obstacleSprites;
    private Clip clip, crashClip;

    public SpaceGame() {
        setTitle("Space Game");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        backgroundStars = new BackgroundStars(WIDTH, HEIGHT, NUMBER_OF_STARS);
        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g);
            }
        };

        // Setting up game panel and labels
        scoreLabel = new JLabel("Score: " + score);
        scoreLabel.setBounds(10, 10, 100, 20);
        scoreLabel.setForeground(Color.BLUE);
        gamePanel.add(scoreLabel);

        healthLabel = new JLabel("Health: " + playerHealth);
        healthLabel.setBounds(WIDTH - 150, 10, 100, 20);
        healthLabel.setForeground(Color.RED);
        gamePanel.add(healthLabel);

        levelLabel = new JLabel("Level: " + level);
        levelLabel.setBounds(WIDTH / 2 - 50, 10, 100, 20);
        levelLabel.setForeground(Color.GREEN);
        gamePanel.add(levelLabel);

        timerLabel = new JLabel("Time: 30");
        timerLabel.setBounds(WIDTH / 2 - 50, 30, 100, 20);
        timerLabel.setForeground(Color.WHITE);
        gamePanel.add(timerLabel);

        add(gamePanel);
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);

        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 20;
        projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
        projectileY = playerY;
        isProjectileVisible = false;
        isGameOver = false;
        isFiring = false;
        shieldActive = false;
        obstacles = new ArrayList<>();
        powerUps = new ArrayList<>();
        random = new Random();
        spaceshipImage = new ImageIcon("spaceship.png").getImage();

        // Load images and sounds
        try {
            spriteSheet = ImageIO.read(new File("obstacles.png"));
            obstacleSprites = new BufferedImage[NUM_SPRITES];
            for (int i = 0; i < NUM_SPRITES; i++) {
                obstacleSprites[i] = spriteSheet.getSubimage(i * SPRITE_WIDTH, 0, SPRITE_WIDTH, SPRITE_HEIGHT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("fire.wav"));
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            AudioInputStream crashStream = AudioSystem.getAudioInputStream(new File("crash.wav"));
            crashClip = AudioSystem.getClip();
            crashClip.open(crashStream);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }

        gameStartTime = System.currentTimeMillis();
        // Game loop timer
        timer = new Timer(20, e -> {
            if (!isGameOver) {
                update();
                gamePanel.repaint();
            }
        });
        timer.start();
    }

    private void activateShield() {
        if (!shieldActive) {
            shieldActive = true;
            shieldStartTime = System.currentTimeMillis();
        }
    }
    private boolean isShieldActive() {
        return shieldActive;
    }

    private void deactivateShield() {
        shieldActive = false;
    }

    private void draw(Graphics g) {
        // Game rendering logic
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);
        backgroundStars.draw(g);
        g.drawImage(spaceshipImage, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT, this);
        if (isProjectileVisible) {
            g.setColor(Color.GREEN);
            g.fillRect(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
        }

        // Draw obstacles
        for (Point obstacle : obstacles) {
            int spriteIndex = random.nextInt(NUM_SPRITES);
            g.drawImage(obstacleSprites[spriteIndex], obstacle.x, obstacle.y, SPRITE_WIDTH, SPRITE_HEIGHT, this);
        }

        // Draw power-ups
        g.setColor(Color.PINK);
        for (Point powerUp : powerUps) {
            g.fillOval(powerUp.x, powerUp.y, POWERUP_SIZE, POWERUP_SIZE);
        }

        if (isGameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Game Over!", WIDTH / 2 - 80, HEIGHT / 2);
        }

        if (levelUpDisplayed) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Level " + level, WIDTH / 2 - 40, HEIGHT / 2 - 40);
        }

        if (shieldActive) {
            g.setColor(new Color(0, 255, 255, 128)); // Cyan color with transparency
            g.fillOval(playerX - 10, playerY - 10, PLAYER_WIDTH + 20, PLAYER_HEIGHT + 20);
        }
    }

    private void update() {
        long elapsedTime = System.currentTimeMillis() - gameStartTime;
        int timeLeft = 30 - (int) (elapsedTime / 1000);
        timerLabel.setText("Time: " + timeLeft);

        if (timeLeft <= 0) {
            levelUp();
            gameStartTime = System.currentTimeMillis();
            levelUpDisplayed = true;
            new Timer(2000, e -> levelUpDisplayed = false).start();
        }

        for (int i = 0; i < obstacles.size(); i++) {
            obstacles.get(i).y += obstacleSpeed;
            if (obstacles.get(i).y > HEIGHT) {
                obstacles.remove(i);
                i--;
            }
        }

        if (Math.random() < 0.02) {
            int obstacleX = random.nextInt(WIDTH - SPRITE_WIDTH);
            obstacles.add(new Point(obstacleX, 0));
        }

        if (isProjectileVisible) {
            projectileY -= PROJECTILE_SPEED;
            if (projectileY < 0) {
                isProjectileVisible = false;
            }
        }

        if (Math.random() < POWERUP_SPAWN_CHANCE) {
            int powerUpX = random.nextInt(WIDTH - POWERUP_SIZE);
            powerUps.add(new Point(powerUpX, 0));
        }

        for (int i = 0; i < powerUps.size(); i++) {
            powerUps.get(i).y += obstacleSpeed;
            if (powerUps.get(i).y > HEIGHT) {
                powerUps.remove(i);
                i--;
            }
        }
        if (isShieldActive() && (System.currentTimeMillis() - shieldStartTime > shieldDuration)) {
            shieldActive = false;  // Deactivate the shield
        }

        Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
        Rectangle projectileRect = new Rectangle(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
        for (int i = 0; i < obstacles.size(); i++) {
            Rectangle obstacleRect = new Rectangle(obstacles.get(i).x, obstacles.get(i).y, SPRITE_WIDTH, SPRITE_HEIGHT);
            if (playerRect.intersects(obstacleRect)) {
                if (!shieldActive) {
                    playerHealth -= 1;
                    healthLabel.setText("Health: " + playerHealth);
                    crashClip.start();
                    crashClip.setFramePosition(0);
                    if (playerHealth <= 0) {
                        isGameOver = true;
                    }
                    obstacles.remove(i);
                    i--;
                } else {
                    // If shield is active, just remove the obstacle without reducing health
                    obstacles.remove(i);
                    i--;
                }
            } else if (isProjectileVisible && projectileRect.intersects(obstacleRect)) {
                score += 10;
                scoreLabel.setText("Score: " + score);
                obstacles.remove(i);
                i--;
                isProjectileVisible = false;
            }
        }

        for (int i = 0; i < powerUps.size(); i++) {
            Rectangle powerUpRect = new Rectangle(powerUps.get(i).x, powerUps.get (i).y, POWERUP_SIZE, POWERUP_SIZE);
            if (playerRect.intersects(powerUpRect)) {
                powerUps.remove(i);
                i--;
                increaseHealth();
            }
        }
    }

    private void playSound() {
        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
        clip.start();
    }

    private void increaseHealth() {
        playerHealth += 1;
        healthLabel.setText("Health: " + playerHealth);
    }

    private void reset() {
        score = 0;
        playerHealth = 1; // Reset health to 1
        healthLabel.setText("Health: " + playerHealth);
        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 20;
        projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
        projectileY = playerY;
        isProjectileVisible = false;
        isFiring = false;
        obstacles.clear();
        powerUps.clear();
        isGameOver = false;
        scoreLabel.setText("Score: " + score);
        gameStartTime = System.currentTimeMillis();
        level = 1;
        levelLabel.setText("Level: " + level);
        obstacleSpeed = INITIAL_OBSTACLE_SPEED;
        gamePanel.requestFocus();
    }

    private void levelUp() {
        level++;
        levelLabel.setText("Level: " + level);
        obstacleSpeed *= 1.2; // Increase speed by 20% each level
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();
        if (keyCode == KeyEvent.VK_LEFT && playerX > 0) {
            playerX -= PLAYER_SPEED;
        } else if (keyCode == KeyEvent.VK_RIGHT && playerX < WIDTH - PLAYER_WIDTH) {
            playerX += PLAYER_SPEED;
        } else if (keyCode == KeyEvent.VK_SPACE && !isFiring) {
            isFiring = true;
            playSound();
            projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
            projectileY = playerY;
            isProjectileVisible = true;
            new Thread(() -> {
                try {
                    Thread.sleep(500); // Cool down before another shot
                    isFiring = false;
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }).start();
        } else if (keyCode == KeyEvent.VK_V) {
            activateShield();
        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            reset();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SpaceGame().setVisible(true));
    }
}



