package com.skillswap.presentation.contrato;

import com.skillswap.presentation.controller.BusquedaController;
import com.skillswap.presentation.controller.CalificacionController;
import com.skillswap.presentation.controller.ContratacionController;
import com.skillswap.presentation.controller.NotificacionController;
import com.skillswap.presentation.controller.PerfilController;
import com.skillswap.presentation.controller.PublicacionController;
import com.skillswap.presentation.controller.RecomendacionController;
import com.skillswap.presentation.controller.WalletController;

import java.util.function.IntSupplier;

/*
 * Lo unico que la interfaz recibe de la aplicacion: los controllers (que solo hablan en DTOs) y dos
 * contadores de los mocks de email y push para mostrar el Adapter en la demo.
 * Lo arma SkillSwapApplication (composition root); la interfaz no sabe como se construyen.
 */
public record Controladores(BusquedaController busqueda,
                           RecomendacionController recomendaciones,
                           ContratacionController contrataciones,
                           CalificacionController calificaciones,
                           PerfilController perfiles,
                           PublicacionController publicaciones,
                           WalletController billetera,
                           NotificacionController notificaciones,
                           IntSupplier emails,
                           IntSupplier push) {

    public int emailsEnviados() {
        return emails.getAsInt();
    }

    public int pushEnviados() {
        return push.getAsInt();
    }
}
