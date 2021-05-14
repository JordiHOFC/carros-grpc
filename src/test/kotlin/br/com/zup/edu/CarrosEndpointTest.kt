package br.com.zup.edu

import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.Before
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

@MicronautTest(transactional = false)
internal class CarrosEndpointTest(
    val grpcClient: CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub,
    val repository: CarroRepository
) {


    @Test
    fun `deve adicionar um novo carro`() {
        repository.deleteAll()
        var request = CarroRequest.newBuilder()
            .setModelo("Carro")
            .setPlaca("HXT-2424")
            .build()

        var reponse = grpcClient.adicionar(request)

        var carro = repository.findById(reponse.id)

        assertTrue(carro.isPresent)
        assertEquals(request.modelo, carro.get().modelo)
        assertEquals(request.placa, carro.get().placa)
    }


    @Test
    fun `nao deve adicionar um carro que possua placa de outro carro ja cadastrado `() {
        var carro = Carro(modelo = "Uno", placa = "HXT-2424")
        repository.save(carro)

        val e = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(
                CarroRequest.newBuilder()
                    .setModelo("Gol")
                    .setPlaca("HXT-2424")
                    .build()
            )
        }

        assertEquals(e.status.code.toStatus(), Status.ALREADY_EXISTS)
        assertEquals(e.status.description, "carro com placa existente")
    }

    @Test
    fun `nao deve adicionar um carro sem modelo e sem placa`() {
        val e = assertThrows<StatusRuntimeException> {
            grpcClient.adicionar(
                CarroRequest.newBuilder()
                    .setModelo(" ")
                    .setPlaca(" ")
                    .build()
            )
        }
        assertEquals(e.status.code.toStatus(), Status.INVALID_ARGUMENT)
        assertEquals(e.status.description, "dados de entrada inválidos")
    }

    @Factory
    class Clients {
        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub? {
            return CarrosGrpcServiceGrpc.newBlockingStub(channel)
        }
    }
}