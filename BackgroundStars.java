import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BackgroundStars {
    private List<Point> stars;
    private Random random;

    public BackgroundStars(int width, int height, int numberOfStars) {
        stars = new ArrayList<>();
        random = new Random();
        generateStars(width, height, numberOfStars);
    }

    public void generateStars(int width, int height, int numberOfStars) {
        stars.clear();
        for (int i = 0; i < numberOfStars; i++) {
            int x = random.nextInt(width);
            int y = random.nextInt(height);
            stars.add(new Point(x, y));
        }
    }

    public void draw(Graphics g) {
        for (Point star : stars) {
            g.setColor(generateRandomColor());
            g.fillOval(star.x, star.y, 3, 3); // Draw each star as a small circle
        }
    }

    private Color generateRandomColor() {
        int red = random.nextInt(256);
        int green = random.nextInt(256);
        int blue = random.nextInt(256);
        return new Color(red, green, blue);
    }
}
