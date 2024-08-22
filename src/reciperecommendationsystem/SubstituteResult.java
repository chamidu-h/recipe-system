/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package reciperecommendationsystem;

/**
 *
 * @author imcnb
 */
public class SubstituteResult {
    private String recipeName;
    private String substitutes;
    private int substituteCount;

    public SubstituteResult(String recipeName, String substitutes, int substituteCount) {
        this.recipeName = recipeName;
        this.substitutes = substitutes;
        this.substituteCount = substituteCount;
    }

    public String getRecipeName() {
        return recipeName;
    }

    public String getSubstitutes() {
        return substitutes;
    }

    public int getSubstituteCount() {
        return substituteCount;
    }
}


