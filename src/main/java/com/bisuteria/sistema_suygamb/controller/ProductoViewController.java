package com.bisuteria.sistema_suygamb.controller;

import java.io.ByteArrayOutputStream;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import java.util.Optional;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.bisuteria.sistema_suygamb.model.Cliente;
import com.bisuteria.sistema_suygamb.model.DetalleVenta;
import com.bisuteria.sistema_suygamb.model.Producto;
import com.bisuteria.sistema_suygamb.model.Venta;
import com.bisuteria.sistema_suygamb.repository.ClienteRepository;
import com.bisuteria.sistema_suygamb.repository.DetalleVentaRepository;
import com.bisuteria.sistema_suygamb.repository.ProductoRepository;
import com.bisuteria.sistema_suygamb.repository.VentaRepository;

@Controller
public class ProductoViewController {

    @Autowired
    private ProductoRepository repo; 

    @Autowired
    private VentaRepository ventaRepo;
    @Autowired
    private DetalleVentaRepository detalleRepo;
    
    private List<DetalleVenta> carrito =
            new ArrayList<>();
    
    //LOGIN
    @GetMapping("/login")
    public String login() {

        return "login";
    }
    // PANEL JEFE
    @GetMapping("/")
    public String inicio(
            @RequestParam(required = false) String buscar,
            Model model){

        List<Producto> productos;

        // BUSCADOR

        if(buscar != null && !buscar.isEmpty()){

            productos =
            repo.findByNombreContainingOrCodigoContaining(
                    buscar,
                    buscar);

        }else{

            productos = repo.findByActivoTrue();
        }

        /* DEBUG PRODUCTOS */

        System.out.println("===== PRODUCTOS =====");

        for(Producto p : productos){

            System.out.println(
                    p.getCodigo()
                    + " -> STOCK: "
                    + p.getStock()
            );
        }

        /* STOCK CRITICO */

        List<Producto> stockCritico = productos.stream()
                .filter(p -> p.getStock() != null
                        && p.getStock() <= 60)
                .toList();

        /* DEBUG STOCK CRITICO */

        System.out.println("===== STOCK CRITICO =====");

        for(Producto p : stockCritico){

            System.out.println(
                    "CRITICO -> "
                    + p.getCodigo()
                    + " = "
                    + p.getStock()
            );
        }

        System.out.println(
                "TOTAL STOCK CRITICO: "
                + stockCritico.size()
        );

        model.addAttribute(
                "stockCritico",
                stockCritico);
        
        // VARIABLES

        double ventasHoy = 0;
        double ventasMes = 0;
        double utilidadHoy = 0;
        double utilidadTotal = 0;
        double ganancias = 0;
        
        LocalDate hoy = LocalDate.now();
        
        for(Venta v : ventaRepo.findAll()){

            if(v.getTotal() != null){

                ganancias += v.getTotal();
            }

            if(v.getDetalles() != null){

                for(DetalleVenta d : v.getDetalles()){

                    if(d.getProducto() != null
                            && d.getProducto().getPrecioCompra() != null
                            && d.getPrecio() != null){

                        double gananciaUnidad =
                                d.getPrecio()
                                - d.getProducto().getPrecioCompra();

                        double utilidadDetalle =
                                gananciaUnidad * d.getCantidad();

                        utilidadTotal += utilidadDetalle;

                        if(v.getFecha() != null
                                && v.getFecha().toLocalDate().equals(hoy)){

                            utilidadHoy += utilidadDetalle;
                        }
                    }
                }
            }
        }

        // LISTA VENTAS

        List<Venta> ventas =
        ventaRepo.findAll();

        // PRODUCTOS MÁS VENDIDOS

        List<Object[]> masVendidos =
        detalleRepo.productosMasVendidos(); 
        
        List<Object[]> ranking = detalleRepo.productosMasVendidos();

        for(Object[] fila : ranking){

            String codigo =
                    (String) fila[0];

            String producto =
                    (String) fila[1];

            Long cantidad =
                    ((Number) fila[2]).longValue();

            System.out.println(
                    codigo + " - "
                    + producto + " -> "
                    + cantidad
            );        
        }
        
        // FECHA ACTUAL
        /*
        LocalDate hoy =
        LocalDate.now();
         */
        // RECORRER VENTAS

        for(Venta v : ventas){
      
            // VALIDAR FECHA

            if(v.getFecha() != null){

                LocalDate fechaVenta =
                v.getFecha().toLocalDate();

                // VENTAS DEL DÍA

                if(fechaVenta.equals(hoy)){

                    ventasHoy += v.getTotal();
                }

                // VENTAS DEL MES

                if(fechaVenta.getMonthValue() == hoy.getMonthValue()
                        && fechaVenta.getYear() == hoy.getYear()){

                    ventasMes += v.getTotal();
                }
            }
        }

        // STOCK BAJO

        long stockBajo = productos.stream()
                .filter(p -> p.getStock() != null
                        && p.getStock() <= 60)
                .count();
        
        

        // CAPITAL TOTAL

        double capitalTotal = 0;

        for(Producto p : productos){

            if(p.getPrecioCompra() != null
                    && p.getStock() != null){

                capitalTotal +=
                p.getPrecioCompra() * p.getStock();
            }
        }

        // ENVIAR DATOS

        model.addAttribute(
                "ventasHoy",
                ventasHoy);

        model.addAttribute(
                "ventasMes",
                ventasMes);
        model.addAttribute(
        		"utilidadHoy", 
        		utilidadHoy);

        model.addAttribute(
                "stockBajo",
                stockBajo);

        model.addAttribute(
                "totalProductos",
                productos.size());

        model.addAttribute(
                "capitalTotal",
                capitalTotal);

        model.addAttribute(
                "utilidadTotal",
                utilidadTotal);

        model.addAttribute(
                "ganancias",
                ganancias);

        model.addAttribute(
                "masVendidos",
                masVendidos);

        model.addAttribute(
                "productos",
                productos);
        
        model.addAttribute(
                "stockCritico",
                stockCritico);        

        return "productos";
    }

    // PANEL EMPLEADO
    // PANEL EMPLEADO
    @GetMapping("/empleado")
    public String empleado(
            @RequestParam(required = false) String buscar,
            Model model){
    	System.out.println("BUSCANDO: " + buscar);

        List<Producto> productos;
        String error = null;

        if(buscar != null && !buscar.trim().isEmpty()){

            Optional<Producto> producto =
                    repo.findByCodigo(buscar.trim());

            if(producto.isPresent()){

                productos = List.of(producto.get());

            }else{

                productos = Collections.emptyList();

                error = "❌ Código de producto no encontrado";
            }

        }else{

            productos = repo.findByActivoTrue();
        }

        List<Producto> stockCritico =
                repo.findByActivoTrue()
                .stream()
                .filter(p -> p.getStock() != null
                        && p.getStock() <= 60)
                .toList();

        model.addAttribute("productos", productos);
        model.addAttribute("error", error);
        model.addAttribute("stockCritico", stockCritico);
        model.addAttribute("stockBajo", stockCritico.size());
        model.addAttribute("carrito", carrito);
        model.addAttribute("totalProductos",
                repo.findByActivoTrue().size());

        return "ventas";
    }
    
    // GUARDAR PRODUCTO
    @PostMapping("/guardar")
    public String guardar(
            @ModelAttribute Producto producto,
            Model model){


        Optional<Producto> existe =
                repo.findByCodigo(
                        producto.getCodigo()
                );


        if(existe.isPresent()){


            long stockBajo = repo.findByActivoTrue()
                    .stream()
                    .filter(p -> p.getStock() != null
                            && p.getStock() <= 60)
                    .count();



            model.addAttribute(
                    "error",
                    "❌ El código "
                    + producto.getCodigo()
                    + " ya existe"
            );



            model.addAttribute(
                    "productos",
                    repo.findByActivoTrue()
            );



            model.addAttribute(
                    "totalProductos",
                    repo.findByActivoTrue().size()
            );



            model.addAttribute(
                    "stockBajo",
                    stockBajo
            );



            return "productos";
        }

        // CALCULAR STOCK AUTOMÁTICAMENTE
        if(producto.getCantidadCaja() != null
                && producto.getCajas() != null){

            producto.setStock(
                    producto.getCantidadCaja()
                    * producto.getCajas()
            );
        }

        repo.save(producto);

        return "redirect:/";
    }
    // ELIMINAR PRODUCTO
    @GetMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id){

        Producto producto =
                repo.findById(id).orElse(null);

        if(producto != null){

            producto.setActivo(false);

            repo.save(producto);
        }

        return "redirect:/";
    }
    // EDITAR PRODUCTO
    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model){

        Producto producto = repo.findById(id).orElse(null);

        model.addAttribute("producto", producto);

        return "editar";
    }

    // ACTUALIZAR PRODUCTO
    @PostMapping("/actualizar")
    public String actualizar(@ModelAttribute Producto producto){

        if(producto.getCantidadCaja() != null
                && producto.getCajas() != null){

            producto.setStock(
                    producto.getCantidadCaja()
                    * producto.getCajas()
            );
        }

        repo.save(producto);

        return "redirect:/";
    }
    
    //HISTORIAL VENTA
    @GetMapping("/historial")
    public String historial(Model model){
        List<Venta> ventas = ventaRepo.findAllConDetalles();
        model.addAttribute("ventas", ventas);
        return "historial";
    }
    
    //GUARDAR LAS VENTAS
    @PostMapping("/guardarVenta")
    public String guardarVenta(
    		 Integer productoId,
    	        Integer cantidad,
    	        String tipoVenta,
    	        String cliente,
    	        String dni,
    	        String comprobante,
    	        String guia,
    	        String direccionEntrega,
    	        String destinatario,
    	        String agencia,
    	        String observacion,
    	        String razonSocial,
    	        String ruc,
    	        String direccionFiscal,
    	        Model model){

        Producto producto = repo.findById(productoId).orElse(null);

        if(producto != null){

            double precio = 0;

            if(tipoVenta.equals("UNIDAD")){

                precio = producto.getPrecioUnidad();

            }else{

                precio = producto.getPrecioMayor();
            }

            // CALCULOS
            // Quitamos el comentario y funciona el igv para lo que es boleta 
            double subtotal = precio * cantidad;
            double igv = 0; //subtotal * 0.18;
            double total = subtotal; //+ igv;
           
            if(comprobante.equals("FACTURA")){

                igv = subtotal * 0.18;
                total = subtotal + igv;
            }
            
            if(cantidad > producto.getStock()){

                model.addAttribute(
                    "error",
                    "❌ Stock insuficiente"
                );

                model.addAttribute(
                    "productos",
                    repo.findByActivoTrue()
                );

                return "ventas";
                
            }
        
            // CREAR VENTA
            
            Venta venta = new Venta();
            venta.setProducto(producto.getNombre());
            venta.setCantidad(cantidad);
            venta.setTipoVenta(tipoVenta);
            venta.setSubtotal(subtotal);
            venta.setIgv(igv);
            venta.setTotal(total);
            
            venta.setDireccionEntrega(direccionEntrega);
            venta.setObservacion(observacion);
            venta.setAgencia(agencia);
            venta.setDestinatario(destinatario); 
            venta.setComprobante(comprobante);
            venta.setGuia(guia);
            venta.setRazonSocial(razonSocial);
            venta.setRuc(ruc);
            venta.setDireccionFiscal(direccionFiscal);
            venta.setCliente(cliente);
            venta.setDni(dni);

            venta.setFecha(LocalDateTime.now());            
            // GENERAR NUMERO COMPROBANTE

            long totalVentas = ventaRepo.count() + 1;

            String numero = "";

            if(comprobante.equals("BOLETA")){

                numero = "B001-" +
                        String.format("%06d", totalVentas);

            }else{

                numero = "F001-" +
                        String.format("%06d", totalVentas);
            }

            venta.setNumeroComprobante(numero);
            System.out.println("================================");
            System.out.println("COMPROBANTE: " + comprobante);
            System.out.println("SUBTOTAL: " + subtotal);
            System.out.println("IGV: " + igv);
            System.out.println("TOTAL: " + total);
            System.out.println("================================");
            ventaRepo.save(venta);

            // ENVIAR DATOS AL TICKET
            model.addAttribute("venta", venta);

            // ABRIR TICKET
            return "ticket";
        }

        return "ventas";
    
    }
    
    //VENTAS - CORREGIDO
    @GetMapping("/ventas")
    public String ventas(Model model){

        List<Producto> productos = repo.findByActivoTrue();

        List<Producto> stockCritico =
                productos.stream()
                         .filter(p -> p.getStock() != null
                                 && p.getStock() <= 60)
                         .toList();

        long stockBajo =
                productos.stream()
                         .filter(p -> p.getStock() != null
                                 && p.getStock() <= 60)
                         .count();

        long totalProductos = productos.size();

        model.addAttribute("productos", productos);
        model.addAttribute("stockBajo", stockBajo);
        model.addAttribute("totalProductos", totalProductos);
        model.addAttribute("stockCritico", stockCritico);
        model.addAttribute("carrito", carrito); // <-- ESTA LINEA TE FALTABA

        return "ventas";
    }
	 // EXPORTAR EXCEL
	    @GetMapping("/exportar-excel")
	    public void exportarExcel(
	            HttpServletResponse response)
	            throws Exception {
	
	
	        response.setContentType(
	                "application/octet-stream");
	
	
	        response.setHeader(
	                "Content-Disposition",
	                "attachment; filename=ventas.xlsx");
	
	
	        List<Venta> ventas =
	                ventaRepo.findAll();
	
	
	        Workbook workbook =
	                new XSSFWorkbook();
	
	
	
	        // ================= HOJAS =================
	
	        Sheet sheetBoletas =
	                workbook.createSheet("Boletas");
	
	
	        Sheet sheetFacturas =
	                workbook.createSheet("Facturas");
	
	
	        Sheet sheetResumen =
	                workbook.createSheet("Resumen");
	
	
	
	        // ================= ESTILO HEADER =================
	
		
	        CellStyle headerStyle =
	                workbook.createCellStyle();
	
	
	        Font headerFont =
	                workbook.createFont();
	
	
	        headerFont.setBold(true);
	        
	
	
	        headerFont.setColor(
	                IndexedColors.WHITE.getIndex());
	
	
	        headerStyle.setFont(headerFont);
	
	
	        headerStyle.setFillForegroundColor(
	                IndexedColors.DARK_BLUE.getIndex());
	
	
	        headerStyle.setFillPattern(
	                FillPatternType.SOLID_FOREGROUND);
	
	
	
	        // ================= WRAP =================
	
	
	        CellStyle wrapStyle =
	                workbook.createCellStyle();
	
	
	        wrapStyle.setWrapText(true);
	
	
	        wrapStyle.setVerticalAlignment(
	                VerticalAlignment.TOP);
	
	
	
	        // ================= CABECERAS =================
	
	
	        crearCabeceraBoleta(sheetBoletas, headerStyle);

	        crearCabeceraFactura(sheetFacturas, headerStyle);
	
	
	
	        int filaBoleta = 5;
	
	        int filaFactura = 5;
	
	
	        double totalGeneral = 0;
	
	
	
	        DateTimeFormatter formato =
	                DateTimeFormatter.ofPattern(
	                        "yyyy-MM-dd HH:mm:ss");
	
	
	
	        // ================= DATOS =================
	
	
	        for(Venta v : ventas){
	
	
	            Sheet hojaDestino;
	
	
	            int filaActual;
	
	
	
	            if("FACTURA".equalsIgnoreCase(
	                    v.getComprobante())){
	
	
	                hojaDestino = sheetFacturas;
	
	
	                filaActual = filaFactura++;
	
	
	            }else{
	
	
	                hojaDestino = sheetBoletas;
	
	
	                filaActual = filaBoleta++;
	
	            }
	
	
	
	            Row row =
	                    hojaDestino.createRow(filaActual);
	
	
	
	            String productos = "";
	
	            String cantidades = "";
	
	            String tiposVenta = "";
	
	
	
	            int item = 1;
	
	
	
	            if(v.getDetalles()!=null){
	
	
	                for(DetalleVenta d : v.getDetalles()){
	
	
	
	                    productos +=
	                            item + ". "
	                            + d.getProducto().getCodigo()
	                            + " - "
	                            + d.getProducto().getNombre()
	                            + "\n";
	
	
	
	                    cantidades +=
	                            "x" + d.getCantidad()
	                            + "\n";
	
	
	
	                    String tipoVenta =
	                            "PERSONALIZADO";
	
	
	
	                    if(d.getPrecio().equals(
	                            d.getProducto().getPrecioMayor())){
	
	
	                        tipoVenta = "MAYOR";
	
	
	                    }else if(d.getPrecio().equals(
	                            d.getProducto().getPrecioUnidad())){
	
	
	                        tipoVenta = "UNIDAD";
	
	                    }
	
	
	
	                    tiposVenta +=
	                            tipoVenta + "\n";
	
	
	
	                    item++;
	
	                }
	
	            }
	
	
	
	
	            row.createCell(0)
	                    .setCellValue(v.getId());
	
	
	            row.createCell(1)
	                    .setCellValue(
	                            v.getFecha().format(formato));
	
	
	            row.createCell(2)
	                    .setCellValue(
	                            v.getCliente()!=null
	                            ? v.getCliente()
	                            : "-");
	
	
	
	            if("FACTURA".equalsIgnoreCase(v.getComprobante())){


	                // FACTURA
	                row.createCell(3)
	                        .setCellValue(
	                                v.getRazonSocial()!=null
	                                ? v.getRazonSocial()
	                                : "-"
	                        );


	                row.createCell(4)
	                        .setCellValue(
	                                v.getRuc()!=null
	                                ? v.getRuc()
	                                : "-"
	                        );


	                row.createCell(5)
	                        .setCellValue(productos);


	                row.createCell(6)
	                        .setCellValue(cantidades);


	                row.createCell(7)
	                        .setCellValue(tiposVenta);


	                row.createCell(8)
	                        .setCellValue(v.getComprobante());


	                row.createCell(9)
	                        .setCellValue(v.getNumeroComprobante());


	                row.createCell(10)
	                        .setCellValue(v.getSubtotal());


	                row.createCell(11)
	                        .setCellValue(v.getIgv());


	                row.createCell(12)
	                        .setCellValue(v.getTotal());



	            }else{


	                // BOLETA (SIN RAZON SOCIAL NI RUC)

	                row.createCell(3)
	                        .setCellValue(productos);


	                row.createCell(4)
	                        .setCellValue(cantidades);


	                row.createCell(5)
	                        .setCellValue(tiposVenta);


	                row.createCell(6)
	                        .setCellValue(v.getComprobante());


	                row.createCell(7)
	                        .setCellValue(v.getNumeroComprobante());


	                row.createCell(8)
	                        .setCellValue(v.getSubtotal());


	                row.createCell(9)
	                        .setCellValue(v.getIgv());


	                row.createCell(10)
	                        .setCellValue(v.getTotal());

	            }
	
	
	            for(Cell c : row){
	
	                c.setCellStyle(wrapStyle);
	
	            }
	
	
	
	            row.setHeightInPoints(100);
	
	
	
	            totalGeneral += v.getTotal();
	
	        }
	
	
	
	        // ================= RESUMEN =================
	
	
	        Row tituloResumen =
	                sheetResumen.createRow(0);


	        tituloResumen.createCell(0)
	                .setCellValue(
	                        "SISTEMA SUYGAMB S.A.C");



	        Row subtituloResumen =
	                sheetResumen.createRow(1);


	        subtituloResumen.createCell(0)
	                .setCellValue(
	                        "RESUMEN DE VENTAS");

	        // ===== CABECERA TABLA =====

	        Row fechaResumen =
	                sheetResumen.createRow(2);


	        fechaResumen.createCell(0)
	                .setCellValue(
	                        "Fecha: "
	                        + LocalDate.now());
	        
	        Row headerResumen =
	                sheetResumen.createRow(4);



	        headerResumen.createCell(0)
	                .setCellValue("Concepto");


	        headerResumen.createCell(1)
	                .setCellValue("Valor");
	        
	        // Aplicar estilo al encabezado
	        headerResumen.getCell(0)
	        .setCellStyle(headerStyle);


			headerResumen.getCell(1)
			        .setCellStyle(headerStyle);
			

			// ===== CALCULOS =====


			double totalVendidoResumen = 0;


			int cantidadVentasResumen = ventas.size();


			int totalBoletasResumen = 0;


			int totalFacturasResumen = 0;



			for(Venta v : ventas){


			    totalVendidoResumen += v.getTotal();



			    if("FACTURA".equalsIgnoreCase(
			            v.getComprobante())){


			        totalFacturasResumen++;


			    }else{


			        totalBoletasResumen++;

			    }

			}



			// ===== DATOS TABLA =====


			Row resumen1 =
			        sheetResumen.createRow(5);


			resumen1.createCell(0)
			        .setCellValue(
			                "Total vendido");


			resumen1.createCell(1)
			        .setCellValue(
			                "S/ "
			                + totalVendidoResumen);




			Row resumen2 =
			        sheetResumen.createRow(6);


			resumen2.createCell(0)
			        .setCellValue(
			                "Cantidad de ventas");


			resumen2.createCell(1)
			        .setCellValue(
			                cantidadVentasResumen);




			Row resumen3 =
			        sheetResumen.createRow(7);


			resumen3.createCell(0)
			        .setCellValue(
			                "Total boletas");


			resumen3.createCell(1)
			        .setCellValue(
			                totalBoletasResumen);




			Row resumen4 =
			        sheetResumen.createRow(8);


			resumen4.createCell(0)
			        .setCellValue(
			                "Total facturas");


			resumen4.createCell(1)
			        .setCellValue(
			                totalFacturasResumen);



			// ===== AJUSTAR COLUMNAS =====


			sheetResumen.autoSizeColumn(0);

			sheetResumen.autoSizeColumn(1);

	
	
	
	        // ================= AJUSTES =================
	
	
	        ajustarHoja(sheetBoletas);
	
	        ajustarHoja(sheetFacturas);
	
	
	
	        // ================= EXPORTAR =================
	
	
	        ServletOutputStream outputStream =
	                response.getOutputStream();
	
	
	        workbook.write(outputStream);
	
	
	        workbook.close();
	
	
	        outputStream.close();
	
	    }
	
	
	
	    // ================= CABECERA =================
	
	
	    private void crearCabeceraBoleta(
	            Sheet sheet,
	            CellStyle headerStyle){


	        sheet.createRow(0)
	                .createCell(0)
	                .setCellValue("SISTEMA SUYGAMB S.A.C");


	        sheet.createRow(1)
	                .createCell(0)
	                .setCellValue("REPORTE DE BOLETAS");


	        sheet.createRow(2)
	                .createCell(0)
	                .setCellValue(
	                        "Fecha: "
	                        + LocalDate.now());


	        Row header =
	                sheet.createRow(4);


	        String[] columnas = {


	                "ID",
	                "Fecha",
	                "Cliente",
	                "Productos",
	                "Cantidades",
	                "Tipo Venta",
	                "Comprobante",
	                "N° Comprobante",
	                "Subtotal",
	                "IGV",
	                "Total"

	        };


	        for(int i=0;i<columnas.length;i++){


	            Cell cell =
	                    header.createCell(i);


	            cell.setCellValue(columnas[i]);


	            cell.setCellStyle(headerStyle);

	        }

	    }
	    
	    private void crearCabeceraFactura(
	            Sheet sheet,
	            CellStyle headerStyle){


	        sheet.createRow(0)
	                .createCell(0)
	                .setCellValue("SISTEMA SUYGAMB S.A.C");


	        sheet.createRow(1)
	                .createCell(0)
	                .setCellValue("REPORTE DE FACTURAS");


	        sheet.createRow(2)
	                .createCell(0)
	                .setCellValue(
	                        "Fecha: "
	                        + LocalDate.now());


	        Row header =
	                sheet.createRow(4);


	        String[] columnas = {


	                "ID",
	                "Fecha",
	                "Cliente",
	                "Razón Social",
	                "RUC",
	                "Productos",
	                "Cantidades",
	                "Tipo Venta",
	                "Comprobante",
	                "N° Comprobante",
	                "Subtotal",
	                "IGV",
	                "Total"

	        };


	        for(int i=0;i<columnas.length;i++){


	            Cell cell =
	                    header.createCell(i);


	            cell.setCellValue(columnas[i]);


	            cell.setCellStyle(headerStyle);

	        }

	    }
	
	
	
	    // ================= FORMATO HOJAS =================
	
	
	    private void ajustarHoja(
	            Sheet sheet){
	
	
	        for(int i=0;i<13;i++){
	
	            sheet.autoSizeColumn(i);
	
	        }
	
	
	        sheet.setColumnWidth(5,15000);
	
	        sheet.setColumnWidth(6,5000);
	
	        sheet.setColumnWidth(7,6000);
	
	
	
	        sheet.setAutoFilter(
	                new org.apache.poi.ss.util.CellRangeAddress(
	                        4,
	                        sheet.getLastRowNum(),
	                        0,
	                        12));
	
	    }
	    
    //EXPORTAR PDF
    @GetMapping("/exportar-pdf")
    public ResponseEntity<byte[]> exportarPDF() throws Exception {

        List<Venta> ventas = ventaRepo.findAll();

        Document document = new Document(PageSize.A4.rotate());

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        PdfWriter.getInstance(document, out);

        document.open();

        Paragraph titulo = new Paragraph(
                "REPORTE COMERCIAL DE VENTAS SUYGAMB S.A.C");

        titulo.setAlignment(Element.ALIGN_CENTER);

        document.add(titulo);

        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(6);

        table.setWidthPercentage(100);

        table.setWidths(new float[]{
                1.5f,
                2.5f,
                2.5f,
                6f,
                2f,
                2f
        });

        table.addCell("ID");
        table.addCell("Fecha");
        table.addCell("Cliente");
        table.addCell("Productos");
        table.addCell("Comprobante");
        table.addCell("Total");

        double totalGeneral = 0;

        for(Venta v : ventas){

            String productos = "";

            if(v.getDetalles() != null){

                for(DetalleVenta d : v.getDetalles()){

                    String tipoVenta = "PERSONALIZADO";

                    if(d.getPrecio().equals(
                            d.getProducto().getPrecioMayor())){

                        tipoVenta = "MAYOR";

                    }else if(d.getPrecio().equals(
                            d.getProducto().getPrecioUnidad())){

                        tipoVenta = "UNIDAD";
                    }

                    productos +=
                            "• "
                            + d.getProducto().getCodigo()
                            + " - "
                            + d.getProducto().getNombre()
                            + " x"
                            + d.getCantidad()
                            + " ("
                            + tipoVenta
                            + ")\n";
                }
            }

            table.addCell(
                    String.valueOf(v.getId())
            );

            table.addCell(
                    v.getFecha() != null
                    ? v.getFecha().format(
                            DateTimeFormatter.ofPattern(
                                    "dd/MM/yyyy HH:mm"))
                    : "-"
            );

            table.addCell(
                    v.getCliente() != null
                    ? v.getCliente()
                    : "-"
            );

            table.addCell(productos);

            table.addCell(
                    v.getNumeroComprobante() != null
                    ? v.getNumeroComprobante()
                    : "-"
            );

            table.addCell(
                    String.format(
                            "S/ %.2f",
                            v.getTotal())
            );

            totalGeneral += v.getTotal();
        }

        document.add(table);

        document.add(new Paragraph(" "));

        Paragraph total = new Paragraph(
                "TOTAL GENERAL VENDIDO: S/ "
                + String.format("%.2f", totalGeneral));

        total.setAlignment(Element.ALIGN_RIGHT);

        document.add(total);

        document.close();

        HttpHeaders headers =
                new HttpHeaders();

        headers.add(
                "Content-Disposition",
                "attachment; filename=reporte_comercial.pdf"
        );

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }
    //EXPORTAR PDF(PDF---TRIBUTARIO)
    @GetMapping("/exportar-pdf-tributario")
    public ResponseEntity<byte[]> exportarPDFTributario() throws Exception {

        List<Venta> ventas = ventaRepo.findAll();

        Document document = new Document();

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        PdfWriter.getInstance(document, out);

        document.open();

        document.add(
                new Paragraph("REPORTE TRIBUTARIO SUNAT")
        );

        document.add(
                new Paragraph("SUYGAMB S.A.C")
        );

        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(8);

        table.setWidthPercentage(100);

        table.addCell("Fecha");
        table.addCell("Razón Social");
        table.addCell("RUC");
        table.addCell("Serie");
        table.addCell("N° Comprobante");
        table.addCell("Subtotal");
        table.addCell("IGV");
        table.addCell("Total");

        double totalGeneral = 0;

        for(Venta v : ventas){

            // SOLO FACTURAS

            if(!"FACTURA".equals(v.getComprobante())){
                continue;
            }

            table.addCell(
                    v.getFecha() != null
                    ? v.getFecha().format(
                            DateTimeFormatter.ofPattern(
                                    "dd/MM/yyyy"))
                    : "-"
            );

            table.addCell(
                    v.getRazonSocial() != null
                    ? v.getRazonSocial()
                    : "-"
            );

            table.addCell(
                    v.getRuc() != null
                    ? v.getRuc()
                    : "-"
            );

            table.addCell(
                    v.getSerie() != null
                    ? v.getSerie()
                    : "-"
            );

            table.addCell(
                    v.getNumeroComprobante() != null
                    ? v.getNumeroComprobante()
                    : "-"
            );

            table.addCell(
                    String.format(
                            "S/ %.2f",
                            v.getSubtotal())
            );

            table.addCell(
                    String.format(
                            "S/ %.2f",
                            v.getIgv())
            );

            table.addCell(
                    String.format(
                            "S/ %.2f",
                            v.getTotal())
            );

            totalGeneral += v.getTotal();
        }

        document.add(table);

        document.add(new Paragraph(" "));

        document.add(
                new Paragraph(
                        "TOTAL FACTURADO: S/ "
                        + String.format("%.2f", totalGeneral)
                )
        );

        document.close();

        HttpHeaders headers =
                new HttpHeaders();

        headers.add(
                "Content-Disposition",
                "attachment; filename=reporte_sunat.pdf"
        );

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }
    
 // AGREGAR CARRITO DE COMPRA - CORREGIDO
    @PostMapping("/agregar-carrito")
    public String agregarCarrito(
            String codigo,
            Integer cantidad,
            String tipoVenta,
            RedirectAttributes redirectAttributes){

        Optional<Producto> productoEncontrado = repo.findByCodigo(codigo);

        if(productoEncontrado.isEmpty()){
            redirectAttributes.addFlashAttribute("error", "❌ Producto no encontrado");
            return "redirect:/ventas";
        }

        Producto producto = productoEncontrado.get();

        if(cantidad == null || cantidad <= 0){
            redirectAttributes.addFlashAttribute("error", "❌ La cantidad debe ser mayor a 0");
            return "redirect:/ventas";
        }

        if(producto.getStock() == null || cantidad > producto.getStock()){
            redirectAttributes.addFlashAttribute("error", "❌ Stock insuficiente. Disponible: " + producto.getStock());
            return "redirect:/ventas";
        }

        // VER SI YA EXISTE EN CARRITO PARA NO DUPLICAR
        boolean encontrado = false;
        for(DetalleVenta d : carrito){
            if(d.getProducto().getId().equals(producto.getId()) && d.getPrecio() == ("UNIDAD".equals(tipoVenta) ? producto.getPrecioUnidad() : producto.getPrecioMayor())){
                d.setCantidad(d.getCantidad() + cantidad);
                d.setSubtotal(d.getCantidad() * d.getPrecio());
                encontrado = true;
                break;
            }
        }

        if(!encontrado){
            double precio = "UNIDAD".equals(tipoVenta) ? producto.getPrecioUnidad() : producto.getPrecioMayor();
            double subtotal = precio * cantidad;
            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(cantidad);
            detalle.setPrecio(precio);
            detalle.setSubtotal(subtotal);
            carrito.add(detalle);
        }

        // CLAVE: redirect para evitar reenvío de formulario
        return "redirect:/ventas";
    }

    // ELIMINAR CARRITO - CORREGIDO
    @GetMapping("/eliminar-carrito/{index}")
    public String eliminarCarrito(@PathVariable int index){
        if(index >= 0 && index < carrito.size()){
            carrito.remove(index);
        }
        return "redirect:/ventas";
    }

    // MÉTODO PARA CARGAR DATOS DE VENTAS

    private void cargarDatosVentas(Model model){


        List<Producto> productos =
                repo.findByActivoTrue();



        // STOCK CRÍTICO

        List<Producto> stockCritico =
                productos.stream()
                .filter(p -> p.getStock() != null
                        && p.getStock() <= 60)
                .toList();



        long stockBajo =
                stockCritico.size();



        model.addAttribute(
                "productos",
                productos
        );


        model.addAttribute(
                "carrito",
                carrito
        );


        model.addAttribute(
                "totalProductos",
                productos.size()
        );


        model.addAttribute(
                "stockBajo",
                stockBajo
        );


        model.addAttribute(
                "stockCritico",
                stockCritico
        );

    }
    
  //FINALIZAR COMPRA - CORREGIDO
    @PostMapping("/finalizar-venta")
    @org.springframework.transaction.annotation.Transactional
    public String finalizarVenta(
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) String dni,
            @RequestParam(required = false) String razonSocial,
            @RequestParam(required = false) String ruc,
            @RequestParam(required = false) String direccionFiscal,
            @RequestParam(required = false) String comprobante,
            @RequestParam(required = false) String guia,
            @RequestParam(required = false) String direccionEntrega,
            @RequestParam(required = false) String destinatario,
            @RequestParam(required = false) String agencia,
            @RequestParam(required = false) String observacion,
            Model model){

        long stockBajo = repo.findByActivoTrue().stream().filter(p -> p.getStock() <= 60).count();
        model.addAttribute("totalProductos", repo.findByActivoTrue().size());
        model.addAttribute("stockBajo", stockBajo);
        model.addAttribute("productos", repo.findByActivoTrue());
        model.addAttribute("carrito", carrito);
        cargarStock(model);

        if(carrito.isEmpty()){
            model.addAttribute("error", "El carrito está vacío");
            return "ventas";
        }
        if("BOLETA".equals(comprobante)){
            if(cliente == null || cliente.trim().isEmpty()){
                model.addAttribute("error", "❌ Ingrese nombre del cliente");
                return "ventas";
            }
            if(dni == null || dni.length() != 8){
                model.addAttribute("error", "❌ DNI inválido");
                return "ventas";
            }
        }
        if("FACTURA".equals(comprobante)){
            if(razonSocial == null || razonSocial.trim().isEmpty()){
                model.addAttribute("error", "❌ Ingrese razón social");
                return "ventas";
            }
            if(ruc == null || ruc.length() != 11){
                model.addAttribute("error", "❌ RUC inválido");
                return "ventas";
            }
        }
        if(comprobante == null || comprobante.trim().isEmpty()){
            model.addAttribute("error", "❌ Seleccione tipo de comprobante");
            return "ventas";
        }
        if(cliente != null && !cliente.trim().isEmpty() && dni != null && !dni.trim().isEmpty()) {
            Optional<Cliente> clienteExistente = clienteRepo.findByDni(dni);
            if(clienteExistente.isEmpty()) {
                Cliente nuevoCliente = new Cliente();
                nuevoCliente.setNombre(cliente);
                nuevoCliente.setDni(dni);
                clienteRepo.save(nuevoCliente);
            }
        }

        // 1. CALCULAR TOTALES PRIMERO (sin tocar la BD)
        double subtotal = 0;
        for(DetalleVenta d : carrito){
            subtotal += d.getSubtotal();
            if(d.getProducto() != null && d.getCantidad() > d.getProducto().getStock()){
                model.addAttribute("error", "❌ Stock insuficiente para: " + d.getProducto().getNombre());
                return "ventas";
            }
        }
        double igv = 0;
        double total = subtotal;
        if("FACTURA".equals(comprobante)){
            igv = subtotal * 0.18;
            total = subtotal + igv;
        }
        
        // 2. CREAR Y GUARDAR LA VENTA PADRE SOLA, SIN DETALLES
        Venta venta = new Venta();
        venta.setSubtotal(subtotal);
        venta.setIgv(igv);
        venta.setTotal(total);
        venta.setCliente(cliente);
        venta.setDni(dni);
        venta.setRazonSocial(razonSocial);
        venta.setRuc(ruc);
        venta.setDireccionFiscal(direccionFiscal);
        venta.setComprobante(comprobante);
        venta.setGuia(guia);
        venta.setDireccionEntrega(direccionEntrega);
        venta.setDestinatario(destinatario);
        venta.setAgencia(agencia);
        venta.setObservacion(observacion);
        venta.setFecha(LocalDateTime.now());

        long totalVentas = ventaRepo.count() + 1;
        String numeroFormateado = String.format("%06d", totalVentas);
        if("BOLETA".equals(comprobante)){
            venta.setSerie("B001");
            venta.setNumeroComprobante("B001-" + numeroFormateado);
        }else{
            venta.setSerie("F001");
            venta.setNumeroComprobante("F001-" + numeroFormateado);
        }
        
        venta = ventaRepo.save(venta); // YA TIENE ID

        // 3. AHORA SI GUARDAR LOS DETALLES CON EL ID DE LA VENTA
        for(DetalleVenta d : carrito){
            d.setVenta(venta);
            Producto producto = d.getProducto();
            if(producto != null){
                producto.setStock(producto.getStock() - d.getCantidad());
                repo.save(producto);
            }
            detalleRepo.save(d);
        }

        carrito = new ArrayList<>();

        if("FACTURA".equals(comprobante)){
            return "redirect:/ticket/electronico/" + venta.getId();
        }else{
            return "redirect:/ticket/simple/" + venta.getId();
        }
    }
    
    //TICKET
    @GetMapping("/ticket/{id}")
    public String verTicket(
            @PathVariable Integer id,
            Model model){

        Venta venta =
                ventaRepo.findById(id).orElse(null);

        model.addAttribute(
                "venta",
                venta);

        return "ticket";
    }
    // TICKET SIMPLE 80MM - ENTREGA RÁPIDA EN MOSTRADOR
    @GetMapping("/ticket/simple/{id}")
    public String ticketSimple(@PathVariable Integer id, Model model){
        Venta venta = ventaRepo.findById(id).orElse(null);
        if(venta == null) return "redirect:/ventas";
        
        model.addAttribute("venta", venta);
        return "ticket-simple-80mm"; // -> templates/ticket-simple-80mm.html
    }

    // BOLETA / FACTURA ELECTRONICA A4 - PARA PDF Y WHATSAPP
    @GetMapping("/ticket/electronico/{id}")
    public String ticketElectronico(@PathVariable Integer id, Model model){
        Venta venta = ventaRepo.findById(id).orElse(null);
        if(venta == null) return "redirect:/ventas";

        model.addAttribute("venta", venta);
        return "boleta-electronica-a4"; // -> templates/boleta-electronica-a4.html
    }
    //Factura / 
    @GetMapping("/ticket/factura/{id}")
    public String facturaPro(@PathVariable Integer id, Model model){
        Venta venta = ventaRepo.findById(id).orElse(null);
        if(venta == null) return "redirect:/ventas";
        model.addAttribute("venta", venta);
        return "factura-profesional-a4";
    }
    //UPLOADS----LOGO
    @Value("${app.upload.logo}")
    private String rutaLogo;
    
    //VER-PRODUCTOS MAS PERO EN OTRA VENTANA EXTRA 
    @GetMapping("/productos-mas-vendidos")
    public String productosMasVendidos(
            Model model){

        model.addAttribute(
                "ranking",
                detalleRepo.obtenerProductosMasVendidos());

        return "productos-mas-vendidos";
    }
    
    //ESTO ES PARA IDENTIFICAR A LOS CLIENTES 
    //CON SOLO PONER EL DNI ACTOMATICAMENTE SALE EL NOMBRE
    @Autowired
    private ClienteRepository clienteRepo;

    @GetMapping("/buscar-cliente")
    @ResponseBody
    public Cliente buscarCliente(
            @RequestParam String dni){

        return clienteRepo
                .findByDni(dni)
                .orElse(null);
    
    }
    @PostMapping("/guardar-cliente")
    @ResponseBody
    public Cliente guardarCliente(
            @RequestParam String dni,
            @RequestParam String nombre){

        Cliente c = clienteRepo.findByDni(dni)
                    .orElse(new Cliente());
        
        c.setDni(dni);
        c.setNombre(nombre);

        return clienteRepo.save(c);
    }
    
    
    //PAGINA PRINCIPAL
    @GetMapping("/dashboard")
    public String dashboard(Model model){

        List<Producto> productos = repo.findByActivoTrue();

        // STOCK CRÍTICO
        List<Producto> stockCritico = productos.stream()
                .filter(p -> p.getStock() != null
                        && p.getStock() <= 60)
                .toList();

        double ventasHoy = 0;
        double ventasMes = 0;
        double utilidadHoy = 0;
        double utilidadTotal = 0;

        LocalDate hoy = LocalDate.now();

        List<Venta> ventas = ventaRepo.findAll();

        for(Venta v : ventas){

            if(v.getDetalles() != null){

                for(DetalleVenta d : v.getDetalles()){

                    if(d.getProducto() != null
                            && d.getProducto().getPrecioCompra() != null
                            && d.getPrecio() != null){

                        double gananciaUnidad =
                                d.getPrecio()
                                - d.getProducto().getPrecioCompra();

                        double utilidadDetalle =
                                gananciaUnidad * d.getCantidad();

                        utilidadTotal += utilidadDetalle;

                        if(v.getFecha() != null
                                && v.getFecha().toLocalDate().equals(hoy)){

                            utilidadHoy += utilidadDetalle;
                        }
                    }
                }
            }

            if(v.getFecha() != null){

                LocalDate fechaVenta =
                        v.getFecha().toLocalDate();

                if(fechaVenta.equals(hoy)){

                    ventasHoy += v.getTotal();
                }

                if(fechaVenta.getMonthValue() == hoy.getMonthValue()
                        && fechaVenta.getYear() == hoy.getYear()){

                    ventasMes += v.getTotal();
                }
            }
        }

        long stockBajo = productos.stream()
                .filter(p -> p.getStock() != null
                        && p.getStock() <= 60)
                .count();

        double capitalTotal = 0;

        for(Producto p : productos){

            if(p.getPrecioCompra() != null
                    && p.getStock() != null){

                capitalTotal +=
                        p.getPrecioCompra() * p.getStock();
            }
        }

        model.addAttribute("ventasHoy", ventasHoy);
        model.addAttribute("ventasMes", ventasMes);
        model.addAttribute("utilidadHoy", utilidadHoy);
        model.addAttribute("stockBajo", stockBajo);
        model.addAttribute("totalProductos", productos.size());
        model.addAttribute("capitalTotal", capitalTotal);
        model.addAttribute("utilidadTotal", utilidadTotal);

        model.addAttribute("stockCritico", stockCritico);

        model.addAttribute(
                "masVendidos",
                detalleRepo.productosMasVendidos());

        return "dashboard";
    }
    private void cargarStock(Model model){

        List<Producto> productos =
                repo.findByActivoTrue();


        long stockBajo =
                productos.stream()
                .filter(p -> p.getStock() != null
                        && p.getStock() <= 60)
                .count();



        List<Producto> stockCritico =
                productos.stream()
                .filter(p -> p.getStock() != null
                        && p.getStock() <= 60)
                .toList();



        model.addAttribute(
                "productos",
                productos
        );


        model.addAttribute(
                "totalProductos",
                productos.size()
        );


        model.addAttribute(
                "stockBajo",
                stockBajo
        );


        model.addAttribute(
                "stockCritico",
                stockCritico
        );


        model.addAttribute(
                "carrito",
                carrito
        );

    }
    //SIRVE PARA QUE EL EMPLEADO NO PUEDA INGRESAR 
    //EN LOS VENTANAS DEL JEFE
    @GetMapping("/acceso-denegado")
    public String accesoDenegado() {
        return "acceso-denegado";
    }
    
}