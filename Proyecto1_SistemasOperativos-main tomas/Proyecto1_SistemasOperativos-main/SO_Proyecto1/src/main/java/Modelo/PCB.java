/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Modelo;
import Modelo.Estado;
/**
 *
 * @author Arturo
 */
public class PCB {
    private int id; // A veces el ID se repite aquí para referencia rápida
    private Estado estado; // "New", "Ready", "Running", "Blocked", "Terminated"
    private int programCounter; // (PC) Instrucción actual
    private int memoryAddressRegister; // (MAR) Dirección de memoria actual
    
    
    public PCB(int id){
        this.id = id;
        this.estado = Estado.NUEVO;
        this.programCounter = 0;
        this.memoryAddressRegister = 0;
    }
}
