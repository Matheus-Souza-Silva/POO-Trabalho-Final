/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package br.com.modelo;

import java.sql.SQLException;

/**
 *
 * @author alves
 */
public class Testcon extends ArbitroDAO {
    public static void main(String[] args) throws SQLException {
        ArbitroDAO cx = new ArbitroDAO();
        cx.abrirBanco();
    }
}
