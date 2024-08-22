
package reciperecommendationsystem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import javax.swing.JOptionPane;



public class DatabaseConnection {
    
    private static DatabaseConnection instance;

    private DatabaseConnection() {
        
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }
    
    
    
    
    private static final String URL = "jdbc:mysql://localhost:3308/recipe_system";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
    

    
        
public List<String> getRecipesByIngredientsWithWeights(List<String> ingredients) throws SQLException {
    Map<String, Integer> recipeWeights = new HashMap<>();
    
    String placeholders = String.join(",", Collections.nCopies(ingredients.size(), "?"));
    String query = "SELECT r.recipe_name, COUNT(i.ingredient_id) as matched_ingredients, " +
                   "(SELECT COUNT(ri.ingredient_id) FROM recipe_ingredients ri WHERE ri.recipe_id = r.recipe_id) as total_ingredients " +
                   "FROM recipes r " +
                   "JOIN recipe_ingredients ri ON r.recipe_id = ri.recipe_id " +
                   "JOIN ingredients i ON ri.ingredient_id = i.ingredient_id " +
                   "WHERE i.ingredient_name IN (" + placeholders + ") " +
                   "GROUP BY r.recipe_name";
    
    try (Connection conn = getConnection(); 
         PreparedStatement stmt = conn.prepareStatement(query)) {
        
        for (int i = 0; i < ingredients.size(); i++) {
            stmt.setString(i + 1, ingredients.get(i));
        }
        
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String recipeName = rs.getString("recipe_name");
                int matchedIngredients = rs.getInt("matched_ingredients");
                int totalIngredients = rs.getInt("total_ingredients");
                int weight = totalIngredients - matchedIngredients; 
                recipeWeights.put(recipeName, weight);
            }
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }

    // Priority queue 
    PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>(Map.Entry.comparingByValue());
    pq.addAll(recipeWeights.entrySet());

    List<String> sortedRecipes = new ArrayList<>();
    while (!pq.isEmpty()) {
        sortedRecipes.add(pq.poll().getKey());
    }

    return sortedRecipes;
}









public List<String> getAllIngredients() throws SQLException {
    List<String> ingredients = new ArrayList<>();
        String query = "SELECT i.ingredient_name FROM available_ingredients ai " +
                       "JOIN ingredients i ON ai.ingredient_id = i.ingredient_id";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ingredients.add(rs.getString("ingredient_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace(); 
        }
        return ingredients;
}


public void addIngredient(String ingredient) throws SQLException {
    String getIdQuery = "SELECT ingredient_id FROM ingredients WHERE ingredient_name = ?";
        String insertQuery = "INSERT INTO available_ingredients (ingredient_id) VALUES (?)";
        try (Connection conn = getConnection();
             PreparedStatement getIdStmt = conn.prepareStatement(getIdQuery);
             PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
            
            getIdStmt.setString(1, ingredient);
            try (ResultSet rs = getIdStmt.executeQuery()) {
                if (rs.next()) {
                    int ingredientId = rs.getInt("ingredient_id");
                    insertStmt.setInt(1, ingredientId);
                    insertStmt.executeUpdate();
                } else {
                    throw new SQLException("Ingredient not found in the database.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); 
        }
}

public void deleteIngredient(String ingredient) throws SQLException {
    String getIdQuery = "SELECT ingredient_id FROM ingredients WHERE ingredient_name = ?";
        String deleteQuery = "DELETE FROM available_ingredients WHERE ingredient_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement getIdStmt = conn.prepareStatement(getIdQuery);
             PreparedStatement deleteStmt = conn.prepareStatement(deleteQuery)) {
            
            getIdStmt.setString(1, ingredient);
            try (ResultSet rs = getIdStmt.executeQuery()) {
                if (rs.next()) {
                    int ingredientId = rs.getInt("ingredient_id");
                    deleteStmt.setInt(1, ingredientId);
                    deleteStmt.executeUpdate();
                } else {
                    throw new SQLException("Ingredient not found in the database.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace(); 
        }
    
}


public List<SubstituteResult> findSubstitutes(String[] inputIngredients) throws SQLException {
    List<SubstituteResult> resultList = new ArrayList<>();

    String inputPlaceholders = String.join(",", Collections.nCopies(inputIngredients.length, "?"));
    String query = "SELECT r.recipe_name, "
             + "GROUP_CONCAT(DISTINCT i2.ingredient_name SEPARATOR ', ') AS substitutes, "
             + "COUNT(DISTINCT i2.ingredient_id) AS substitute_count "
             + "FROM recipe_ingredients ri1 "
             + "JOIN recipes r ON ri1.recipe_id = r.recipe_id "
             + "JOIN recipe_ingredients ri2 ON r.recipe_id = ri2.recipe_id "
             + "JOIN ingredients i2 ON ri2.ingredient_id = i2.ingredient_id "
             + "JOIN ingredients i1 ON ri1.ingredient_id = i1.ingredient_id "
             + "WHERE i1.ingredient_name IN (" + inputPlaceholders + ") "
             + "AND i2.ingredient_name NOT IN (" + inputPlaceholders + ") "
             + "GROUP BY r.recipe_name "
             + "ORDER BY substitute_count ASC";


    try (Connection conn = getConnection();
         PreparedStatement stmt = conn.prepareStatement(query)) {

        
        for (int i = 0; i < inputIngredients.length; i++) {
            stmt.setString(i + 1, inputIngredients[i]);
            stmt.setString(inputIngredients.length + i + 1, inputIngredients[i]);
        }

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String recipeName = rs.getString("recipe_name");
                String substitutes = rs.getString("substitutes");
                int substituteCount = rs.getInt("substitute_count");
                resultList.add(new SubstituteResult(recipeName, substitutes, substituteCount));
            }
        }
    }

    return resultList;
}








public String getRecipeDescription(String recipeName) throws SQLException {
    String description = null;
    String query = "SELECT description FROM recipes WHERE recipe_name = ?";
    
    try (Connection conn = getConnection();
         PreparedStatement stmt = conn.prepareStatement(query)) {
        stmt.setString(1, recipeName);
        try (ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                description = rs.getString("description");
            }
        }
    }
    catch (SQLException e) {
        e.printStackTrace(); 
    }
    return description;
}







    



}

