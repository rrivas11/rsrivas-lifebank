package com.lifebank.source.lbsourcesvc.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifebank.source.lbsourcesvc.pojo.cliente.AddBeneficiaryRequest;
import com.lifebank.source.lbsourcesvc.pojo.cliente.UpdateMailRequest;
import com.lifebank.source.lbsourcesvc.pojo.login.LoginRequest;
import com.lifebank.source.lbsourcesvc.pojo.transaction.SetTransactionRequest;
import com.lifebank.source.lbsourcesvc.process.*;
import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/*  Autor: Ricardo Salvador Rivas Franco
    Descripcion: Servicios de LifeBank
    Funciones: - Gestionar lista de beneficiarios (Favoritos)
               - Realizar transferencias a productos propidos ( Prestamos, Cuentas de ahorro, Tarjetas de credito)
               - Realizar transferencias a productos de Terceros ( Prestamos, Cuentas de ahorro, Tarjetas de credito)
               - Generacion de Token de validacion al iniciar sesion.
*/

@RestController
@PropertySource("classpath:application.properties")
@RequestMapping("${service.url.lifebank}")
public class Controller {
    @Autowired
    private Environment env;
    @Autowired
    private ProductProcess productProcess = new ProductProcess();
    @Autowired
    private LoginProcess loginProcess = new LoginProcess();
    @Autowired
    private TransactionHistoryProcess tranctionHistoryProcess = new TransactionHistoryProcess();
    @Autowired
    private TransactionProcess transactionProcess = new TransactionProcess();
    @Autowired
    private BeneficiarioProcess beneficiarioProcess = new BeneficiarioProcess();
    @Autowired
    private GenerateErrorResponse generateErrorResponse = new GenerateErrorResponse();

    private Date date;
    private DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    private ObjectMapper mapper = new ObjectMapper();
    private Logger log;

    public Controller() {
        this.log = LoggerFactory.getLogger(getClass());
    }


    //Endpoint que valida usuario y contraseña.
    @PostMapping("${app-properties.controller.login}")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            date = new Date();
            MDC.put("function", "login");
            MDC.put("date", dateFormat.format(date));
            log.info("request recibido: "+ mapper.writeValueAsString(loginRequest)); //OJO Quitar contraseña de los logs

            return loginProcess.authenticationProcess(loginRequest.getUser(),loginRequest.getPass());
        }catch (Exception e){
            log.error("Hubo un error es login, en la línea {} en el método {}, detalle del error {}",e.getStackTrace()[0].getLineNumber(),e.getStackTrace()[0].getMethodName(),e);
            return new ResponseEntity<>(generateErrorResponse.getGeneralError(),HttpStatus.BAD_REQUEST);
        }

    }

    //Endpoint para obtener los productos de un cliente
    @GetMapping("${app-properties.controller.products}")
    public ResponseEntity<?> getProducts(@RequestHeader("authorization") String token) {
        try {
            date = new Date();
            MDC.put("function", "getProducts");
            MDC.put("date", dateFormat.format(date));

            return productProcess.process(token);
        }catch (Exception e){
            log.error("Hubo un error en getProducts, en la línea {} en el método {}, detalle del error {}",e.getStackTrace()[0].getLineNumber(),e.getStackTrace()[0].getMethodName(),e);
            return new ResponseEntity<>(generateErrorResponse.getGeneralError(),HttpStatus.BAD_REQUEST);
        }

    }

    //Endpoint para obtener los productos de un cliente
    @GetMapping("${app-properties.controller.transactions}")
    public ResponseEntity<?> getTransactions(@PathVariable("accountID") String idProd, @RequestParam(value="start", required = true) String start,
                                  @RequestParam(value="end", required = true) String end,
                                  @RequestHeader("authorization") String token) {
            try {
                date = new Date();
                MDC.put("function", "getTransactions");
                MDC.put("date", dateFormat.format(date));
                log.info("getTransactions request recibido: accountID: "+ idProd +", start: "+ start +", end: "+ end);
                return  tranctionHistoryProcess.process(idProd,start,end,token);
            }catch (Exception e){
                log.error("Hubo un error en getTransactions, en la línea {} en el método {}, detalle del error {}",e.getStackTrace()[0].getLineNumber(),e.getStackTrace()[0].getMethodName(),e);
                return new ResponseEntity<>(generateErrorResponse.getGeneralError(),HttpStatus.BAD_REQUEST);
            }

    }


    //Endpoint para realizar una transaccion a productos propios del cliente
    @PostMapping("${app-properties.controller.transaction-p}")
    public ResponseEntity<?> setTransactionP(@RequestBody SetTransactionRequest request, @RequestHeader("authorization") String token) {
        try {
            date = new Date();
            MDC.put("function", "setTransactionP");
            MDC.put("date", dateFormat.format(date));
            log.info("setTransactionP request recibido" + mapper.writeValueAsString(request));
            return  transactionProcess.processP(request, token);
        } catch (Exception e) {
            log.error("Hubo un error en setTransactionP, en la línea {} en el método {}, detalle del error {}", e.getStackTrace()[0].getLineNumber(), e.getStackTrace()[0].getMethodName(), e);
            return new ResponseEntity<>(generateErrorResponse.getGeneralError(),HttpStatus.BAD_REQUEST);
        }
    }

    //Endpoint para realizar una transaccion a productos de terceros
    @PostMapping("${app-properties.controller.transaction-t}")
    public ResponseEntity<?> setTransactionT(@RequestBody SetTransactionRequest request, @RequestHeader("authorization") String token) {
        try {
            date = new Date();
            MDC.put("function", "setTransactionT");
            MDC.put("date", dateFormat.format(date));
            log.info("setTransactionT request recibido" + mapper.writeValueAsString(request));
            return transactionProcess.processT(request, token);
        } catch (Exception e) {
            log.error("Hubo un error en setTransactionT, en la línea {} en el método {}, detalle del error {}", e.getStackTrace()[0].getLineNumber(), e.getStackTrace()[0].getMethodName(), e);
            return new ResponseEntity<>(generateErrorResponse.getGeneralError(),HttpStatus.BAD_REQUEST);
        }
    }




    //Endpoint para actualizar el E-mail de un beneficiario agregado en lista de favoritos.
    @PatchMapping("${app-properties.controller.update-mail}")
    public ResponseEntity<Object> updateMail(@PathVariable("beneficiaryID") String beneficiaryID, @RequestBody UpdateMailRequest request , @RequestHeader("authorization") String token) {
        try {
            date = new Date();
            MDC.put("function", "updateMail");
            MDC.put("date", dateFormat.format(date));
            log.info("updateMail request recibido" + mapper.writeValueAsString(request));
            return beneficiarioProcess.updateProcess(request, token,beneficiaryID);
        } catch (Exception e) {
            log.error("Hubo un error en updateMail, en la línea {} en el método {}, detalle del error {}", e.getStackTrace()[0].getLineNumber(), e.getStackTrace()[0].getMethodName(), e);
            return new ResponseEntity<>(generateErrorResponse.getGeneralError(),HttpStatus.BAD_REQUEST);
        }
    }

    //Endpoint para eliminar beneficiario.
    @DeleteMapping("${app-properties.controller.delete-beneficiary}")
    public ResponseEntity<Object> deleteBeneficiary(@PathVariable("beneficiaryID") String beneficiaryID, @RequestHeader("authorization") String token) {
        try {
            date = new Date();
            MDC.put("function", "deleteBeneficiary");
            MDC.put("date", dateFormat.format(date));
            log.info("deleteBeneficiary beneficiaryID: " + beneficiaryID);
            return beneficiarioProcess.deleteProcess(token,beneficiaryID);
        } catch (Exception e) {
            log.error("Hubo un error en deleteBeneficiary, en la línea {} en el método {}, detalle del error {}", e.getStackTrace()[0].getLineNumber(), e.getStackTrace()[0].getMethodName(), e);
            return new ResponseEntity<>(generateErrorResponse.getGeneralError(),HttpStatus.BAD_REQUEST);
        }
    }

    //Endpoint para agrear beneficiario y su cuenta a lista de favoritos
    @PostMapping("${app-properties.controller.add-beneficiary}")
    public ResponseEntity<Object> addBeneficiary(@RequestBody AddBeneficiaryRequest request, @RequestHeader("authorization") String token) {
        try {
            date = new Date();
            MDC.put("function", "deleteBeneficiary");
            MDC.put("date", dateFormat.format(date));
            log.info("addBeneficiary request recibido" + mapper.writeValueAsString(request));
            return beneficiarioProcess.addProcess(request,token);
        } catch (Exception e) {
            log.error("Hubo un error en addBeneficiary, en la línea {} en el método {}, detalle del error {}", e.getStackTrace()[0].getLineNumber(), e.getStackTrace()[0].getMethodName(), e);
            return new ResponseEntity<>(generateErrorResponse.getGeneralError(),HttpStatus.BAD_REQUEST);
        }
    }


}
